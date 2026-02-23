#! /bin/bash

# Script to run TS tests against the Java backend
# It
# - launches a temporary PostgreSQL container
# - runs the migrations
# - starts the Java backend in background
# - runs the JS tests
# - stops and removes the container
# - stops the backend

# Function to clean up the containers
CLEANUP_CALLED=false
cleanup() {
  if [ "$CLEANUP_CALLED" = true ]; then
    return
  fi
  echo "Cleaning up..."
  CLEANUP_CALLED=true

  if [ -n "$NC_PID" ] && kill -0 $NC_PID 2>/dev/null; then
    kill $NC_PID >/dev/null 2>&1
  fi

  if [ "$(docker ps -q -f name=$RANDOM_NAME)" ]; then
    docker stop $RANDOM_NAME >/dev/null 2>&1
  fi

  if [ "$(docker ps -q -f name=ryuk)" ]; then
    docker stop ryuk >/dev/null 2>&1
  fi

  if [ -n "$BACKEND_PID" ] && kill -0 $BACKEND_PID 2>/dev/null; then
    kill $BACKEND_PID >/dev/null 2>&1
  fi
}

# Set up a trap to call the cleanup function on EXIT, SIGINT, and SIGTERM
trap cleanup EXIT SIGINT SIGTERM

# Search for a free port to bind the temporary PG container
BASE_PORT=1234
INCREMENT=1

PORT=$BASE_PORT
IS_FREE=$(netstat -taln | grep $PORT)

while [[ -n "$IS_FREE" ]]; do
  PORT=$((PORT + INCREMENT))
  IS_FREE=$(netstat -taln | grep $PORT)
  # Sleep 100ms
  sleep 0.1
done

# Generate a random name for the temporary PG container
RANDOM_NAME="pg_test_$(date +%s)"

LABEL="nittei_testing=true"

echo ""
echo "########################"
echo "Building Java backend..."
echo "########################"
echo ""

mvn -q -f java/pom.xml -pl nittei-app -am -DskipTests install

# Launch the resource reaper (like testcontainers)
docker run -d --name ryuk --rm -v /var/run/docker.sock:/var/run/docker.sock -e RYUK_VERBOSE=true -e RYUK_PORT=8080 -p 8080:8080 testcontainers/ryuk:0.8.1 >/dev/null 2>&1

# Keep the connection open and send the label to Ryuk
TIMEOUT=60
(
  echo "label=${LABEL}"
  # Keep the connection open to Ryuk and read the ACK response
  while [ $((TIMEOUT--)) -gt 0 ]; do
    sleep 1
  done
) | nc localhost 8080 &
>/dev/null 2>&1
NC_PID=$!

echo ""
echo "#######################"
echo "Launching containers..."
echo "#######################"
echo ""

# Launch a PG container
docker run --rm -d -l ${LABEL} --name $RANDOM_NAME -p $PORT:5432 -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=nittei postgres:15.4-alpine >/dev/null 2>&1

# Set DATABASE_URL (migrations) and backend env variables
export DATABASE_URL="postgres://postgres:postgres@localhost:${PORT}/nittei"
export NITTEI__PG__DATABASE_URL="$DATABASE_URL"
export NITTEI_PG_DATABASE_URL="jdbc:postgresql://127.0.0.1:${PORT}/nittei?user=postgres&password=postgres"
export NITTEI_PG_SKIP_MIGRATIONS=true
export NITTEI_CREATE_ACCOUNT_SECRET_CODE="create_account_dev_secret"

# Wait for PostgreSQL to be ready
RETRIES=5
until
  docker exec $RANDOM_NAME pg_isready >/dev/null 2>&1 ||
    [ $((RETRIES--)) -eq 0 ]
do
  sleep 1
done

# Wait a bit more
sleep 1

# Run the migrations
cd crates/infra && sqlx migrate run && cd ../..

#### FRONTEND ####

# Search for a free port to bind the temporary backend server
BASE_PORT=1234
INCREMENT=1

PORT=$BASE_PORT
IS_FREE=$(netstat -taln | grep $PORT)

while [[ -n "$IS_FREE" ]]; do
  PORT=$((PORT + INCREMENT))
  IS_FREE=$(netstat -taln | grep $PORT)
done

# JS client reads NITTEI__HTTP_PORT; Java app reads NITTEI_HTTP_PORT
export NITTEI__HTTP_PORT=$PORT
export NITTEI_HTTP_PORT=$PORT

echo ""
echo "##################################################"
echo "Starting Java backend server for frontend tests..."
echo "##################################################"
echo ""

if [ -n "$DEBUG" ]; then
  mvn -f java/nittei-app/pom.xml org.springframework.boot:spring-boot-maven-plugin:3.5.0:run &
else
  mvn -q -f java/nittei-app/pom.xml org.springframework.boot:spring-boot-maven-plugin:3.5.0:run >/dev/null 2>&1 &
fi

# Save the PID of the backend server
BACKEND_PID=$!

# Wait for backend server to be ready
RETRIES=20
until
  curl localhost:$PORT/api/v1/healthcheck >/dev/null 2>&1 ||
    [ $((RETRIES--)) -eq 0 ]
do
  sleep 1
done

if ! curl localhost:$PORT/api/v1/healthcheck >/dev/null 2>&1; then
  echo ""
  echo "############################################"
  echo "Java backend did not become ready in time."
  echo "############################################"
  echo ""
  exit 1
fi

echo ""
echo "#########################"
echo "Running frontend tests..."
echo "#########################"
echo ""

# Run JS tests
pnpm run test

# Store result
RESULT=$?

if [ $RESULT -ne 0 ]; then
  echo ""
  echo "#################"
  echo "Some tests failed!"
  echo "#################"
  echo ""
  exit $RESULT
fi

echo ""
echo "#################"
echo "All tests passed!"
echo "#################"
echo ""

# The cleanup function will be called automatically
