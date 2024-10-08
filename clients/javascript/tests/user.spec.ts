import {
  type INitteiClient,
  NitteiClient,
  Frequenzy,
  type INitteiUserClient,
} from '../lib'
import { setupUserClient } from './helpers/fixtures'
import { v4 } from 'uuid'

describe('User API', () => {
  let userId: string
  let calendarId: string
  let accountClient: INitteiClient
  let client: INitteiUserClient
  let unauthClient: INitteiClient

  beforeAll(async () => {
    const data = await setupUserClient()
    client = data.userClient
    accountClient = data.accountClient
    userId = data.userId
    unauthClient = await NitteiClient({
      nitteiAccount: data.accountId,
    })
    const calendarRes = await client.calendar.create({ timezone: 'UTC' })
    if (!calendarRes.data) {
      throw new Error('Calendar not created')
    }
    calendarId = calendarRes.data.calendar.id
  })

  it('should create user', async () => {
    let res = await accountClient.user.create()
    expect(res.status).toBe(201)
    if (!res.data) {
      throw new Error('User not created')
    }
    const { user } = res.data
    const userId = user.id

    res = await accountClient.user.find(userId)
    expect(res.status).toBe(200)
    if (!res.data) {
      throw new Error('User not found')
    }
    expect(res.data.user.id).toBe(userId)

    res = await accountClient.user.remove(userId)
    expect(res.status).toBe(200)

    res = await accountClient.user.find(userId)
    expect(res.status).toBe(404)
  })

  it('should create a 2nd user, and provide the ID', async () => {
    const userId = v4()
    let res = await accountClient.user.create({
      userId,
    })
    expect(res.status).toBe(201)
    if (!res.data) {
      throw new Error('User not created')
    }
    const { user } = res.data

    expect(user.id).toBe(userId)

    res = await accountClient.user.find(userId)
    expect(res.status).toBe(200)
    if (!res.data) {
      throw new Error('User not found')
    }
    expect(res.data.user.id).toBe(userId)

    res = await accountClient.user.remove(userId)
    expect(res.status).toBe(200)

    res = await accountClient.user.find(userId)
    expect(res.status).toBe(404)
  })

  it('should not show any freebusy with no events', async () => {
    const res = await accountClient.user.freebusy(userId, {
      endTime: new Date(1000 * 60 * 60 * 24 * 4),
      startTime: new Date(10),
      calendarIds: [calendarId],
    })
    expect(res.status).toBe(200)
    if (!res.data) {
      throw new Error('Freebusy not found')
    }
    expect(res.data.busy.length).toBe(0)
  })

  it('should show correct freebusy with a single event in calendar', async () => {
    const event = await client.events.create({
      calendarId,
      duration: 1000 * 60 * 60,
      startTime: new Date(0),
      busy: true,
      recurrence: {
        freq: Frequenzy.Daily,
        interval: 1,
        count: 100,
      },
    })
    if (!event.data) {
      throw new Error('Event not created')
    }

    const res = await unauthClient.user.freebusy(userId, {
      endTime: new Date(1000 * 60 * 60 * 24 * 4),
      startTime: new Date(10),
      calendarIds: [calendarId],
    })
    if (!res.data) {
      throw new Error('Freebusy not found')
    }
    expect(res.data.busy.length).toBe(3)

    await client.events.remove(event.data.event.id)
  })

  it('should show correct freebusy with multiple events in calendar', async () => {
    const event1 = await client.events.create({
      calendarId,
      duration: 1000 * 60 * 60,
      startTime: new Date(0),
      busy: true,
      recurrence: {
        freq: Frequenzy.Daily,
        interval: 1,
        count: 100,
      },
    })
    if (!event1.data) {
      throw new Error('Event not created')
    }
    const event2 = await client.events.create({
      calendarId,
      duration: 1000 * 60 * 60,
      startTime: new Date(1000 * 60 * 60 * 4),
      busy: true,
      recurrence: {
        freq: Frequenzy.Daily,
        interval: 1,
        count: 100,
      },
    })
    if (!event2.data) {
      throw new Error('Event not created')
    }
    const event3 = await client.events.create({
      calendarId,
      duration: 1000 * 60 * 60,
      startTime: new Date(0),
      busy: false,
      recurrence: {
        freq: Frequenzy.Daily,
        interval: 2,
        count: 100,
      },
    })
    if (!event3.data) {
      throw new Error('Event not created')
    }

    const res = await unauthClient.user.freebusy(userId, {
      endTime: new Date(1000 * 60 * 60 * 24 * 4),
      startTime: new Date(0),
      calendarIds: [calendarId],
    })
    if (!res.data) {
      throw new Error('Freebusy not found')
    }

    expect(res.data.busy.length).toBe(8)

    for (const e of [event1, event2, event3]) {
      if (!e.data) {
        throw new Error('Event not created')
      }
      await client.events.remove(e.data.event.id)
    }
  })
})
