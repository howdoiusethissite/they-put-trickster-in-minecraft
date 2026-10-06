# The Trickster (Fabric 26.3)

Adds the Trickster from Dead by Daylight to Minecraft.

## Content

### The Trickster (mob)
- Uses the normal player model with a custom skin: `src/main/resources/assets/trickster/textures/entity/trickster.png`.
  The skin in the repo is a placeholder. Drop your own 64x64 skin over that file. If your skin uses slim (3px) arms,
  set `SLIM_ARMS = true` in `TricksterRenderer.java`.
- Wild Tricksters spawn rarely at night and hunt players. Up close they swing the Polished Head Smasher. From range
  they throw 8-knife volleys, alternating hands.
- **Taming:** right click him with any music disc (1 in 3 chance per disc, the disc is used up). Once tamed he
  follows you, sits when you right click him, and fights whatever you fight.
- **Boredom meter:** fills while you stand still doing nothing (no moving, swinging or using items). When full he
  throws knives at you for about 5 seconds. These knives are capped below the last laceration stack and never take you
  below half a heart, so they can't kill you. Right clicking him with a music disc (not used up) clears his boredom.
- **Attention meter:** drains while you aren't looking at him and refills while he's on your screen. At 0 he chases
  you and beats you with his bat until you look at him long enough to bring it back up to 40%.
- Both meters show in the top-left corner while your Trickster is within 32 blocks. They pause while he sits.

### Throwing Knives
- Recipe: iron ingot on top, iron / pink dye / iron in the middle, stick on the bottom.
- 28 knives per magazine; the pack starts with a full magazine plus 60 spare knives. No ammo crafting needed.
- Hold right click to throw; each knife alternates between your right and left hand in first person.
- When the magazine is empty, right click reloads from the spare knives (2 seconds).
- Knives that stick in the ground can be picked back up and go back into the spare pile.

### Laceration
- Every knife hit adds one laceration stack. At 8 the target dies instantly (bypasses armor).
- The meter shows above the head of anything that has been hit, under your crosshair when you look at it, and above
  your hotbar when you are the one being lacerated. Stacks drain one per second after 8 seconds without a hit.

### Polished Head Smasher
- A heavy bat with extra knockback. Dropped by the Trickster (35%) or crafted.

## Building

Requires Java 25.

```
./gradlew build
```

The mod jar ends up in `build/libs/`.

`./gradlew runClientGameTest` boots a test world and saves screenshots of the mod's visuals to
`build/run/clientGameTest/screenshots`.

## Screenshots

Taken by the headless game test (placeholder skin).

![Trickster](docs/screenshots/trickster.png)
![First-person knives](docs/screenshots/knives_first_person.png)
![Laceration meter](docs/screenshots/laceration.png)
![Boredom and attention meters](docs/screenshots/tamed_meters.png)
![Knife volley](docs/screenshots/trickster_volley.png)
