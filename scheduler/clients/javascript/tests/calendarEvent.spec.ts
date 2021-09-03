import { INettuClient, NettuClient, Frequenzy, INettuUserClient } from "../lib";
import { setupUserClient } from "./helpers/fixtures";

describe.only("CalendarEvent API", () => {
  let calendarId: string;
  let userId: string;
  let client: INettuUserClient;
  let unauthClient: INettuClient;
  beforeAll(async () => {
    const data = await setupUserClient();
    client = data.userClient;
    unauthClient = NettuClient({ nettuAccount: data.accountId });
    const calendarRes = await client.calendar.create({
      timezone: "UTC",
    });
    calendarId = calendarRes.data!.calendar.id;
    userId = data.userId;
  });

  it("should not let unauthenticated user create event", async () => {
    const res = await unauthClient.events.create(userId, {
      calendarId,
      duration: 1000,
      startTs: 1000,
    });

    expect(res.status).toBe(401);
  });

  it("should let authenticated user create event", async () => {
    const res = await client.events.create({
      calendarId,
      duration: 1000,
      startTs: 1000,
    });
    expect(res.status).toBe(201);
  });

  it("should create daily event and retrieve instances", async () => {
    const count = 10;
    let res = await client.events.create({
      calendarId,
      duration: 1000,
      startTs: 1000,
      recurrence: {
        freq: Frequenzy.Daily,
        interval: 1,
        count,
      },
    });
    const eventId = res.data!.event.id;
    expect(res.status).toBe(201);
    let res2 = await client.events.getInstances(eventId, {
      startTs: 20,
      endTs: 1000 * 60 * 60 * 24 * (count + 1),
    });
    let instances = res2.data!.instances;
    expect(instances.length).toBe(count);

    // Query after instances are finished
    let res3 = await client.events.getInstances(eventId, {
      startTs: 1000 * 60 * 60 * 24 * (count + 1),
      endTs: 1000 * 60 * 60 * 24 * (count + 30),
    });
    instances = res3.data!.instances;
    expect(instances.length).toBe(0);
  });

  it("should create exception for calendar event", async () => {
    const count = 10;
    let res = await client.events.create({
      calendarId,
      duration: 1000,
      startTs: 1000,
      recurrence: {
        freq: Frequenzy.Daily,
        interval: 1,
        count,
      },
    });
    const event = res.data!.event;
    const eventId = event.id;

    const getInstances = async () => {
      const res = await client.events.getInstances(eventId, {
        startTs: 20,
        endTs: 1000 * 60 * 60 * 24 * (count + 1),
      });
      return res.data!.instances;
    };
    const instancesBeforeException = await getInstances();
    expect(instancesBeforeException.length).toBe(count);

    // do create exception
    const res2 = await client.events.update(eventId, {
      recurrence: event.recurrence,
      exdates: [event.startTs + 24 * 60 * 60 * 1000],
    });
    expect(res2.status).toBe(200);

    const instancesAfterException = await getInstances();
    expect(instancesAfterException.length).toBe(
      instancesBeforeException.length - 1
    );
  });

  it("updating calendar event start time removes exception", async () => {
    const count = 10;
    let res = await client.events.create({
      calendarId,
      duration: 1000,
      startTs: 1000,
      recurrence: {
        freq: Frequenzy.Daily,
        interval: 1,
        count,
      },
    });
    const event = res.data!.event;
    const eventId = event.id;

    const getInstances = async () => {
      const res = await client.events.getInstances(eventId, {
        startTs: 20,
        endTs: 1000 * 60 * 60 * 24 * (count + 1),
      });
      return res.data!.instances;
    };
    const instancesBeforeException = await getInstances();
    // do create exception
    const res2 = await client.events.update(eventId, {
      recurrence: event.recurrence,
      exdates: [event.startTs + 24 * 60 * 60 * 1000],
    });
    expect(res2.status).toBe(200);

    const instancesAfterException = await getInstances();
    expect(instancesAfterException.length).toBe(
      instancesBeforeException.length - 1
    );
    await client.events.update(eventId, {
      recurrence: event.recurrence,
      startTs: event.startTs + 24 * 60 * 60 * 1000,
    });
    const instancesAfterExceptionDeleted = await getInstances();
    expect(instancesAfterExceptionDeleted.length).toBe(
      instancesBeforeException.length
    );
  });
});
