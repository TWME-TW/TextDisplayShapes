import mineflayer from 'mineflayer'

const port = Number(process.env.TDS_E2E_PORT ?? 25579)
const timeoutMs = 45_000

function waitForMessage(bot, expected) {
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      bot.removeListener('messagestr', listener)
      reject(new Error(`Timed out waiting for message: ${expected}`))
    }, timeoutMs)
    const listener = (message) => {
      if (!message.includes(expected)) return
      clearTimeout(timeout)
      bot.removeListener('messagestr', listener)
      resolve(message)
    }
    bot.on('messagestr', listener)
  })
}

function waitForEntity(bot, entityId) {
  return new Promise((resolve, reject) => {
    const current = bot.entities[entityId]
    if (current) {
      resolve(current)
      return
    }
    const timeout = setTimeout(() => {
      bot.removeListener('entitySpawn', listener)
      reject(new Error(`Timed out waiting for entity ${entityId}`))
    }, timeoutMs)
    const listener = (entity) => {
      if (entity.id !== entityId) return
      clearTimeout(timeout)
      bot.removeListener('entitySpawn', listener)
      resolve(entity)
    }
    bot.on('entitySpawn', listener)
  })
}

function waitForMetadata(bot, entity, predicate, description) {
  return new Promise((resolve, reject) => {
    if (predicate(entity)) {
      resolve(entity)
      return
    }
    const timeout = setTimeout(() => {
      bot.removeListener('entityUpdate', listener)
      reject(new Error(`Timed out waiting for ${description}`))
    }, timeoutMs)
    const listener = (updated) => {
      if (updated.id !== entity.id || !predicate(updated)) return
      clearTimeout(timeout)
      bot.removeListener('entityUpdate', listener)
      resolve(updated)
    }
    bot.on('entityUpdate', listener)
  })
}

const bot = mineflayer.createBot({
  host: '127.0.0.1',
  port,
  username: 'TDSClient',
  version: '1.21.11',
  auth: 'offline'
})

const fatal = (error) => {
  console.error(error)
  process.exitCode = 1
  bot.quit('e2e failed')
}

try {
  await new Promise((resolve, reject) => {
    const timeout = setTimeout(() => fail(new Error('Timed out waiting for Mineflayer spawn')), timeoutMs)
    const cleanup = () => {
      clearTimeout(timeout)
      bot.removeListener('spawn', spawned)
      bot.removeListener('error', fail)
      bot.removeListener('kicked', kicked)
    }
    const spawned = () => {
      cleanup()
      resolve()
    }
    const fail = (error) => {
      cleanup()
      reject(error)
    }
    const kicked = (reason) => fail(new Error(`Mineflayer was kicked: ${reason}`))
    bot.once('spawn', spawned)
    bot.once('error', fail)
    bot.once('kicked', kicked)
  })

  bot.once('error', fatal)
  bot.once('kicked', reason => fatal(new Error(`Mineflayer was kicked: ${reason}`)))

  const packetTrace = []
  bot._client.on('packet', (data, metadata) => {
    packetTrace.push({ name: metadata.name, entityId: data?.entityId })
  })

  const originY = bot.entity.position.y
  const readyMessage = waitForMessage(bot, 'TDS_READY:')
  bot.chat('/tdstest')
  const ready = await readyMessage
  const entityId = Number(ready.substring(ready.indexOf('TDS_READY:') + 'TDS_READY:'.length).trim())
  if (!Number.isInteger(entityId)) {
    throw new Error(`Invalid TextDisplayShapes entity ID in message: ${ready}`)
  }

  const entity = await waitForEntity(bot, entityId)
  if (entity.name !== 'text_display') {
    throw new Error(`Expected text_display entity, received ${entity.name}`)
  }
  const translationIndex = bot.registry.entitiesByName.text_display.metadataKeys.indexOf('translation')
  await waitForMetadata(
    bot,
    entity,
    current => Number.isFinite(current.metadata[translationIndex]?.x),
    'initial Text Display translation'
  )
  const initialTranslationX = entity.metadata[translationIndex].x
  packetTrace.length = 0

  await waitForMessage(bot, 'TDS_RELOCATED')
  await waitForMetadata(
    bot,
    entity,
    current => Math.abs(current.metadata[translationIndex]?.x - (initialTranslationX - 1)) < 0.01,
    'rebased Text Display translation'
  )

  const bundledRebase = packetTrace.some((packet, index) =>
    packet.name === 'bundle_delimiter' &&
    packetTrace[index + 1]?.name === 'entity_metadata' &&
    packetTrace[index + 1]?.entityId === entityId &&
    packetTrace[index + 2]?.name === 'sync_entity_position' &&
    packetTrace[index + 3]?.name === 'bundle_delimiter'
  )
  if (!bundledRebase) {
    throw new Error(`Expected atomic root-anchor rebase bundle: ${JSON.stringify(packetTrace)}`)
  }

  const keys = bot.registry.entitiesByName.text_display.metadataKeys
  const durationIndex = keys.indexOf('transformation_interpolation_duration')
  const startDeltaIndex = keys.indexOf('transformation_interpolation_start_delta_ticks')
  const scaleIndex = keys.indexOf('scale')
  const colorIndex = keys.indexOf('background_color')
  const posRotIndex = keys.indexOf('pos_rot_interpolation_duration')
  const initialScaleX = entity.metadata[scaleIndex].x
  packetTrace.length = 0

  await waitForMessage(bot, 'TDS_ANIMATED')
  await waitForMetadata(
    bot,
    entity,
    current => current.metadata[durationIndex] === 5 &&
      current.metadata[startDeltaIndex] === 0 &&
      Math.abs(current.metadata[scaleIndex]?.x - initialScaleX * 2) < 0.01 &&
      (current.metadata[colorIndex] >>> 0) === 0x7864FF64,
    'interpolated geometry and color update on the same entity'
  )
  if (bot.entities[entityId] !== entity) {
    throw new Error('Animated update must reuse the existing Text Display entity')
  }
  const animationBundled = packetTrace.some((packet, index) =>
    packet.name === 'bundle_delimiter' &&
    packetTrace[index + 1]?.name === 'entity_metadata' &&
    packetTrace[index + 1]?.entityId === entityId)
  if (!animationBundled) {
    throw new Error(`Expected bundled animation update: ${JSON.stringify(packetTrace)}`)
  }

  const translated = await waitForMessage(bot, 'TDS_TRANSLATED:')
  const anchorId = Number(translated.substring(translated.indexOf('TDS_TRANSLATED:') + 'TDS_TRANSLATED:'.length).trim())
  const anchor = await waitForEntity(bot, anchorId)
  await waitForMetadata(bot, anchor, current => current.metadata[posRotIndex] === 3,
    'root anchor teleport duration')
  const deadline = Date.now() + timeoutMs
  while (Math.abs(anchor.position.y - (originY + 1)) > 0.01) {
    if (Date.now() > deadline) {
      throw new Error(`Root anchor did not move up by one block: ${anchor.position}`)
    }
    await new Promise(resolve => setTimeout(resolve, 50))
  }

  console.log(JSON.stringify({
    animation: { interpolationDuration: 5, scaleX: entity.metadata[scaleIndex].x },
    translate: { anchorId, teleportDuration: 3, y: anchor.position.y },
    entityId,
    type: entity.name,
    translation: entity.metadata[translationIndex],
    rootAnchorRelocation: true,
    bundle: true
  }))
  bot.quit('e2e complete')
} catch (error) {
  fatal(error)
}
