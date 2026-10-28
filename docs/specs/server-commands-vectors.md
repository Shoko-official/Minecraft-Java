# Commandes : données de l'arbre et sondes

Statut : brouillon
Jalon : M8
Risque : B

Compagnon de `server-commands.md`. Les références Rn renvoient aux règles de cette spec.

## Outil et commandes

Les données ci-dessous viennent de `cmd_ref.py` (lecteur du rapport `commands.json`, Python 3 sans dépendance, texte intégral en annexe). Il ne réutilise aucun code du jeu. Avec `REF=.cache/reference/26.3` :

```bash
python -I cmd_ref.py table                  # décomptes par commande racine (R1)
python -I cmd_ref.py levels                 # niveaux (R7)
python -I cmd_ref.py nested                 # exigences imbriquées (R7)
python -I cmd_ref.py redirects              # redirections (R3)
python -I cmd_ref.py parsers                # analyseurs utilisés (R1, R15)
python -I cmd_ref.py usage NOM              # chemins exécutables d'une commande (R26 à R30)
python -I cmd_ref.py gamerules              # règles de /gamerule (server-game-loop.md R40)
python -I cmd_ref.py lang PREFIXE           # clés de traduction (R13, R16, R24, R26 à R30)
python -I cmd_ref.py registry minecraft:command_argument_type   # identifiants des analyseurs
python -X utf8 -I build_cmds.py             # tableaux du catalogue et des analyseurs de server-commands.md
```

Statut des valeurs : tout ce qui suit est **D** (extrait de l'arbre ou du fichier de langue) sauf les mentions explicites. Les résultats attendus des sondes sont provisoires.


## Décomptes et niveaux (R1, R7)

```
name | level | executable paths | nodes
advancement | gamemasters | 12 | 25
attribute | gamemasters | 12 | 24
ban | admins | 2 | 3
ban-ip | admins | 2 | 3
banlist | admins | 3 | 3
bossbar | gamemasters | 25 | 39
clear | gamemasters | 4 | 4
clone | gamemasters | 104 | 123
compute | gamemasters | 9 | 21
damage | gamemasters | 5 | 10
data | gamemasters | 390 | 752
datapack | gamemasters | 10 | 17
debug | admins | 3 | 5
defaultgamemode | gamemasters | 1 | 2
deop | admins | 1 | 2
dialog | gamemasters | 2 | 6
difficulty | gamemasters | 5 | 5
effect | gamemasters | 10 | 13
enchant | gamemasters | 2 | 4
execute | gamemasters | 44 | 289
experience | gamemasters | 8 | 15
fetchprofile | gamemasters | 3 | 7
fill | gamemasters | 12 | 15
fillbiome | gamemasters | 2 | 6
forceload | gamemasters | 7 | 10
function | gamemasters | 8 | 13
gamemode | gamemasters | 2 | 3
gamerule | gamemasters | 236 | 237
give | gamemasters | 2 | 4
help | all | 2 | 2
item | gamemasters | 38 | 103
jfr | owners | 2 | 3
kick | admins | 2 | 3
kill | gamemasters | 2 | 2
list | all | 2 | 2
locate | gamemasters | 3 | 7
loot | gamemasters | 70 | 121
me | all | 1 | 2
msg | all | 1 | 3
op | admins | 1 | 2
pardon | admins | 1 | 2
pardon-ip | admins | 1 | 2
particle | gamemasters | 7 | 10
perf | owners | 2 | 3
place | gamemasters | 13 | 20
playsound | gamemasters | 67 | 68
posteffect | gamemasters | 4 | 11
publish | owners | 3 | 3
random | all | 12 | 16
recipe | gamemasters | 4 | 9
reload | gamemasters | 1 | 1
return | gamemasters | 2 | 4
ride | gamemasters | 2 | 5
rotate | gamemasters | 4 | 8
save-all | owners | 2 | 2
save-off | owners | 1 | 1
save-on | owners | 1 | 1
say | gamemasters | 1 | 2
schedule | gamemasters | 4 | 8
scoreboard | gamemasters | 30 | 70
seed | gamemasters | 1 | 1
setblock | gamemasters | 5 | 7
setidletimeout | admins | 1 | 2
setworldspawn | gamemasters | 3 | 3
spawnpoint | gamemasters | 4 | 4
spectate | gamemasters | 3 | 3
spreadplayers | gamemasters | 2 | 10
stop | owners | 1 | 1
stopsound | gamemasters | 24 | 26
stopwatch | gamemasters | 5 | 10
summon | gamemasters | 3 | 4
swing | gamemasters | 8 | 8
tag | gamemasters | 3 | 7
team | gamemasters | 28 | 45
teammsg | all | 1 | 2
teleport | gamemasters | 8 | 12
tell | all | 0 | 1
tellraw | gamemasters | 1 | 3
test | gamemasters | 41 | 48
tick | admins | 9 | 12
time | gamemasters | 19 | 30
title | gamemasters | 6 | 14
tm | all | 0 | 1
tp | gamemasters | 0 | 1
transfer | admins | 3 | 4
trigger | all | 3 | 6
unpublish | owners | 1 | 1
version | gamemasters | 1 | 1
w | all | 0 | 1
waypoint | gamemasters | 6 | 13
weather | gamemasters | 6 | 7
whitelist | admins | 6 | 9
worldborder | gamemasters | 10 | 20
xp | gamemasters | 0 | 1
total nodes below the root: 2474
```

```
gamemasters 63 advancement attribute bossbar clear clone compute damage data datapack defaultgamemode dialog difficulty effect enchant execute experience fetchprofile fill fillbiome forceload function gamemode gamerule give item kill locate loot particle place playsound posteffect recipe reload return ride rotate say schedule scoreboard seed setblock setworldspawn spawnpoint spectate spreadplayers stopsound stopwatch summon swing tag team teleport tellraw test time title tp version waypoint weather worldborder xp
admins 13 ban ban-ip banlist debug deop kick op pardon pardon-ip setidletimeout tick transfer whitelist
all 10 help list me msg random teammsg tell tm trigger w
owners 8 jfr perf publish save-all save-off save-on stop unpublish
```

Exigences de permission imbriquées :

```
datapack create -> owners
debug function -> admins
random reset -> gamemasters
random roll range sequence -> gamemasters
random value range sequence -> gamemasters
```

Redirections :

```
109 -> execute
2 -> msg
1 -> teammsg
1 -> teleport
1 -> experience
```

## Analyseurs utilisés (R1, R15)

```
114 brigadier:bool
47 brigadier:double
46 brigadier:float
168 brigadier:integer
17 brigadier:string
116 minecraft:block_pos
11 minecraft:block_predicate
2 minecraft:block_state
5 minecraft:column_pos
16 minecraft:component
48 minecraft:context_float_provider
48 minecraft:context_int_provider
1 minecraft:dialog
6 minecraft:dimension
146 minecraft:entity
4 minecraft:entity_anchor
1 minecraft:feature
2 minecraft:float_range
5 minecraft:function
6 minecraft:game_profile
2 minecraft:gamemode
1 minecraft:heightmap
1 minecraft:hex_color
4 minecraft:int_range
5 minecraft:item_predicate
2 minecraft:item_slot
21 minecraft:item_stack
14 minecraft:loot_modifier
2 minecraft:loot_predicate
14 minecraft:loot_table
7 minecraft:message
5 minecraft:nbt_compound_tag
114 minecraft:nbt_path
15 minecraft:nbt_tag
28 minecraft:objective
1 minecraft:objective_criteria
1 minecraft:operation
1 minecraft:particle
11 minecraft:resource
12 minecraft:resource_key
79 minecraft:resource_location
5 minecraft:resource_or_tag
1 minecraft:resource_or_tag_key
4 minecraft:resource_selector
5 minecraft:rotation
27 minecraft:score_holder
1 minecraft:scoreboard_slot
28 minecraft:slot_source
2 minecraft:style
2 minecraft:swing_animation
1 minecraft:swizzle
5 minecraft:team
2 minecraft:team_color
1 minecraft:template_mirror
1 minecraft:template_rotation
18 minecraft:time
1 minecraft:uuid
2 minecraft:vec2
22 minecraft:vec3
distinct parsers: 59
```

## Syntaxes des commandes de rang A (R26 à R30)

Un chemin par ligne : `<nom:analyseur>` pour un argument, le mot seul pour un littéral. `gamerule` (236 chemins) est décrit par `cmd_ref.py gamerules`.

### help

```
help
help <command:string>
```

### list

```
list
list uuids
```

### say

```
say <message:message>
```

### me

```
me <action:message>
```

### msg

```
msg <targets:entity> <message:message>
```

### teammsg

```
teammsg <message:message>
```

### tellraw

```
tellraw <targets:entity> <message:component>
```

### stop

```
stop
```

### save-all

```
save-all
save-all flush
```

### save-off

```
save-off
```

### save-on

```
save-on
```

### op

```
op <targets:game_profile>
```

### deop

```
deop <targets:game_profile>
```

### kick

```
kick <targets:entity>
kick <targets:entity> <reason:message>
```

### ban

```
ban <targets:game_profile>
ban <targets:game_profile> <reason:message>
```

### ban-ip

```
ban-ip <target:string>
ban-ip <target:string> <reason:message>
```

### banlist

```
banlist
banlist ips
banlist players
```

### pardon

```
pardon <targets:game_profile>
```

### pardon-ip

```
pardon-ip <target:string>
```

### whitelist

```
whitelist add <targets:game_profile>
whitelist list
whitelist off
whitelist on
whitelist reload
whitelist remove <targets:game_profile>
```

### setidletimeout

```
setidletimeout <minutes:integer>
```

### transfer

```
transfer <hostname:string>
transfer <hostname:string> <port:integer>
transfer <hostname:string> <port:integer> <players:entity>
```

### version

```
version
```

### seed

```
seed
```

### tick

```
tick freeze
tick query
tick rate <rate:float>
tick sprint stop
tick sprint <time:time>
tick step
tick step stop
tick step <time:time>
tick unfreeze
```

### time

```
time add <time:time>
time of <clock:resource> add <time:time>
time of <clock:resource> pause
time of <clock:resource> query time
time of <clock:resource> query <timeline:resource>
time of <clock:resource> query <timeline:resource> repetition
time of <clock:resource> rate <rate:float>
time of <clock:resource> resume
time of <clock:resource> set <time:time>
time of <clock:resource> set <timemarker:resource_location>
time pause
time query gametime
time query time
time query <timeline:resource>
time query <timeline:resource> repetition
time rate <rate:float>
time resume
time set <time:time>
time set <timemarker:resource_location>
```

### difficulty

```
difficulty
difficulty easy
difficulty hard
difficulty normal
difficulty peaceful
```

### defaultgamemode

```
defaultgamemode <gamemode:gamemode>
```

### gamemode

```
gamemode <gamemode:gamemode>
gamemode <gamemode:gamemode> <target:entity>
```

### teleport

```
teleport <destination:entity>
teleport <location:vec3>
teleport <targets:entity> <destination:entity>
teleport <targets:entity> <location:vec3>
teleport <targets:entity> <location:vec3> facing entity <facingEntity:entity>
teleport <targets:entity> <location:vec3> facing entity <facingEntity:entity> <facingAnchor:entity_anchor>
teleport <targets:entity> <location:vec3> facing <facingLocation:vec3>
teleport <targets:entity> <location:vec3> <rotation:rotation>
```

### setworldspawn

```
setworldspawn
setworldspawn <pos:block_pos>
setworldspawn <pos:block_pos> <rotation:rotation>
```

### spawnpoint

```
spawnpoint
spawnpoint <targets:entity>
spawnpoint <targets:entity> <pos:block_pos>
spawnpoint <targets:entity> <pos:block_pos> <rotation:rotation>
```

### weather

```
weather clear
weather clear <duration:time>
weather rain
weather rain <duration:time>
weather thunder
weather thunder <duration:time>
```

## Règles de `/gamerule` (R5)

```
advance_time | bool | 
advance_weather | bool | 
allow_entering_nether_using_portals | bool | 
block_drops | bool | 
block_explosion_drop_decay | bool | 
command_block_output | bool | 
command_blocks_work | bool | 
drowning_damage | bool | 
elytra_movement_check | bool | 
ender_pearls_vanish_on_death | bool | 
entity_drops | bool | 
fall_damage | bool | 
fire_damage | bool | 
fire_spread_radius_around_player | integer | {"min": -1}
forgive_dead_players | bool | 
freeze_damage | bool | 
global_sound_events | bool | 
immediate_respawn | bool | 
keep_inventory | bool | 
lava_source_conversion | bool | 
limited_crafting | bool | 
locator_bar | bool | 
log_admin_commands | bool | 
max_block_modifications | integer | {"min": 1}
max_command_forks | integer | {"min": 0}
max_command_sequence_length | integer | {"min": 0}
max_entity_cramming | integer | {"min": 0}
max_minecart_speed | integer | {"max": 1000, "min": 1}
max_snow_accumulation_height | integer | {"max": 8, "min": 0}
mob_drops | bool | 
mob_explosion_drop_decay | bool | 
mob_griefing | bool | 
natural_health_regeneration | bool | 
player_movement_check | bool | 
players_nether_portal_creative_delay | integer | {"min": 0}
players_nether_portal_default_delay | integer | {"min": 0}
players_sleeping_percentage | integer | {"min": 0}
projectiles_can_break_blocks | bool | 
pvp | bool | 
raids | bool | 
random_tick_speed | integer | {"min": 0}
reduced_debug_info | bool | 
respawn_radius | integer | {"min": 0}
send_command_feedback | bool | 
show_advancement_messages | bool | 
show_death_messages | bool | 
spawn_mobs | bool | 
spawn_monsters | bool | 
spawn_patrols | bool | 
spawn_phantoms | bool | 
spawn_wandering_traders | bool | 
spawn_wardens | bool | 
spawner_blocks_work | bool | 
spectators_generate_chunks | bool | 
spread_vines | bool | 
tnt_explodes | bool | 
tnt_explosion_drop_decay | bool | 
universal_anger | bool | 
water_source_conversion | bool | 
literal children: 118 (each rule appears with and without the minecraft: prefix)
```

## Sélecteurs et erreurs d'analyse (R13, R16)

```
argument.entity.selector.allEntities = All entities
argument.entity.selector.allPlayers = All players
argument.entity.selector.missing = Missing selector type
argument.entity.selector.nearestEntity = Nearest entity
argument.entity.selector.nearestPlayer = Nearest player
argument.entity.selector.not_allowed = Selector not allowed
argument.entity.selector.randomPlayer = Random player
argument.entity.selector.self = Current entity
argument.entity.selector.unknown = Unknown selector type '%s'
```

```
argument.entity.options.advancements.description = Players with advancements
argument.entity.options.distance.description = Distance to entity
argument.entity.options.distance.negative = Distance cannot be negative
argument.entity.options.dx.description = Entities between x and x + dx
argument.entity.options.dy.description = Entities between y and y + dy
argument.entity.options.dz.description = Entities between z and z + dz
argument.entity.options.gamemode.description = Players with game mode
argument.entity.options.inapplicable = Option '%s' isn't applicable here
argument.entity.options.level.description = Experience level
argument.entity.options.level.negative = Level shouldn't be negative
argument.entity.options.limit.description = Maximum number of entities to return
argument.entity.options.limit.toosmall = Limit must be at least 1
argument.entity.options.mode.invalid = Invalid or unknown game mode '%s'
argument.entity.options.name.description = Entity name
argument.entity.options.nbt.description = Entities with NBT
argument.entity.options.predicate.description = Custom predicate
argument.entity.options.scores.description = Entities with scores
argument.entity.options.sort.description = Sort the entities
argument.entity.options.sort.irreversible = Invalid or unknown sort type '%s'
argument.entity.options.tag.description = Entities with tag
argument.entity.options.team.description = Entities on team
argument.entity.options.type.description = Entities of type
argument.entity.options.type.invalid = Invalid or unknown entity type '%s'
argument.entity.options.unknown = Unknown option '%s'
argument.entity.options.unterminated = Expected end of options
argument.entity.options.valueless = Expected value for option '%s'
argument.entity.options.x_rotation.description = Entity's x rotation
argument.entity.options.x.description = x position
argument.entity.options.y_rotation.description = Entity's y rotation
argument.entity.options.y.description = y position
argument.entity.options.z.description = z position
```

```
argument.entity.invalid = Invalid name or UUID
argument.entity.notfound.entity = No entity was found
argument.entity.notfound.player = No player was found
argument.entity.options.advancements.description = Players with advancements
argument.entity.options.distance.description = Distance to entity
argument.entity.options.distance.negative = Distance cannot be negative
argument.entity.options.dx.description = Entities between x and x + dx
argument.entity.options.dy.description = Entities between y and y + dy
argument.entity.options.dz.description = Entities between z and z + dz
argument.entity.options.gamemode.description = Players with game mode
argument.entity.options.inapplicable = Option '%s' isn't applicable here
argument.entity.options.level.description = Experience level
argument.entity.options.level.negative = Level shouldn't be negative
argument.entity.options.limit.description = Maximum number of entities to return
argument.entity.options.limit.toosmall = Limit must be at least 1
argument.entity.options.mode.invalid = Invalid or unknown game mode '%s'
argument.entity.options.name.description = Entity name
argument.entity.options.nbt.description = Entities with NBT
argument.entity.options.predicate.description = Custom predicate
argument.entity.options.scores.description = Entities with scores
argument.entity.options.sort.description = Sort the entities
argument.entity.options.sort.irreversible = Invalid or unknown sort type '%s'
argument.entity.options.tag.description = Entities with tag
argument.entity.options.team.description = Entities on team
argument.entity.options.type.description = Entities of type
argument.entity.options.type.invalid = Invalid or unknown entity type '%s'
argument.entity.options.unknown = Unknown option '%s'
argument.entity.options.unterminated = Expected end of options
argument.entity.options.valueless = Expected value for option '%s'
argument.entity.options.x_rotation.description = Entity's x rotation
argument.entity.options.x.description = x position
argument.entity.options.y_rotation.description = Entity's y rotation
argument.entity.options.y.description = y position
argument.entity.options.z.description = z position
argument.entity.selector.allEntities = All entities
argument.entity.selector.allPlayers = All players
argument.entity.selector.missing = Missing selector type
argument.entity.selector.nearestEntity = Nearest entity
argument.entity.selector.nearestPlayer = Nearest player
argument.entity.selector.not_allowed = Selector not allowed
argument.entity.selector.randomPlayer = Random player
argument.entity.selector.self = Current entity
argument.entity.selector.unknown = Unknown selector type '%s'
argument.entity.toomany = Only one entity is allowed, but the provided selector allows more than one
```

```
command.compute.result.named.exact = %s returned value %s
command.compute.result.named.invalid = %s returned invalid value (%s)
command.compute.result.named.rounded = %s returned value %s (rounded to %s)
command.compute.result.unnamed.exact = Number provider returned value %s
command.compute.result.unnamed.invalid = Number provider returned invalid value (%s)
command.compute.result.unnamed.rounded = Number provider returned value %s (rounded to %s)
command.context.here = <--[HERE]
command.context.parse_error = %s at position %s: %s
command.exception = Could not parse command: %s
command.expected.separator = Expected whitespace to end one argument, but found trailing data
command.failed = An unexpected error occurred while trying to execute that command
command.forkLimit = Maximum number of contexts (%s) reached
command.trailing_data = Trailing data found: %s
command.unknown.argument = Incorrect argument for command
command.unknown.command = Unknown or incomplete command. See below for error
```

```
permissions.requires.entity = An entity is required to run this command here
permissions.requires.player = A player is required to run this command here
```

## Textes des commandes de rang A (R24, R26 à R30)

```
commands.help.failed = Unknown command or insufficient permissions
```

```
commands.list.nameAndId = %s (%s)
commands.list.players = There are %s of a max of %s players online: %s
```

```
commands.message.display.incoming = %s whispers to you: %s
commands.message.display.outgoing = You whisper to %s: %s
```

```
commands.message.display.incoming = %s whispers to you: %s
commands.message.display.outgoing = You whisper to %s: %s
```

```
commands.teammsg.failed.noteam = You must be on a team to message your team
```

```
chat.type.admin = [%s: %s]
chat.type.advancement.challenge = %s has completed the challenge %s
chat.type.advancement.goal = %s has reached the goal %s
chat.type.advancement.task = %s has made the advancement %s
chat.type.announcement = [%s] %s
chat.type.emote = * %s %s
chat.type.team.hover = Message Team
chat.type.team.sent = -> %s <%s> %s
chat.type.team.text = %s <%s> %s
chat.type.text = <%s> %s
chat.type.text.narrate = %s says %s
```

```
commands.op.failed = Nothing changed. The player is already an operator
commands.op.success = Made %s a server operator
```

```
commands.deop.failed = Nothing changed. The player is not an operator
commands.deop.success = Made %s no longer a server operator
```

```
commands.kick.owner.failed = Cannot kick the server owner in a LAN game
commands.kick.singleplayer.failed = Cannot kick in an offline singleplayer game
commands.kick.success = Kicked %s: %s
```

```
commands.ban.failed = Nothing changed. The player is already banned
commands.ban.success = Banned %s: %s
commands.banip.failed = Nothing changed. That IP is already banned
commands.banip.info = This ban affects %s player(s): %s
commands.banip.invalid = Invalid IP address or unknown player
commands.banip.success = Banned IP %s: %s
commands.banlist.entry = %s was banned by %s: %s
commands.banlist.entry.unknown = (Unknown)
commands.banlist.list = There are %s ban(s):
commands.banlist.none = There are no bans
```

```
commands.pardon.failed = Nothing changed. The player isn't banned
commands.pardon.success = Unbanned %s
commands.pardonip.failed = Nothing changed. That IP isn't banned
commands.pardonip.invalid = Invalid IP address
commands.pardonip.success = Unbanned IP %s
```

```
commands.whitelist.add.failed = Player is already whitelisted
commands.whitelist.add.success = Added %s to the whitelist
commands.whitelist.alreadyOff = Whitelist is already turned off
commands.whitelist.alreadyOn = Whitelist is already turned on
commands.whitelist.disabled = Whitelist is now turned off
commands.whitelist.enabled = Whitelist is now turned on
commands.whitelist.list = There are %s whitelisted player(s): %s
commands.whitelist.none = There are no whitelisted players
commands.whitelist.reloaded = Reloaded the whitelist
commands.whitelist.remove.failed = Player is not whitelisted
commands.whitelist.remove.success = Removed %s from the whitelist
```

```
commands.setidletimeout.success = The player idle timeout is now %s minute(s)
commands.setidletimeout.success.disabled = The player idle timeout is now disabled
```

```
commands.transfer.error.no_players = Must specify at least one player to transfer
commands.transfer.success.multiple = Transferring %s players to %s:%s
commands.transfer.success.single = Transferring %s to %s:%s
```

```
commands.version.build_time = build_time = %s
commands.version.data = data = %s
commands.version.header = Server version info:
commands.version.id = id = %s
commands.version.name = name = %s
commands.version.pack.data = pack_data = %s
commands.version.pack.resource = pack_resource = %s
commands.version.protocol = protocol = %s (%s)
commands.version.series = series = %s
commands.version.stable.no = stable = no
commands.version.stable.yes = stable = yes
```

```
commands.seed.success = Seed: %s
```

```
commands.stop.stopping = Stopping the server
commands.stopsound.success.source.any = Stopped all '%s' sounds
commands.stopsound.success.source.sound = Stopped sound '%s' on source '%s'
commands.stopsound.success.sourceless.any = Stopped all sounds
commands.stopsound.success.sourceless.sound = Stopped sound '%s'
commands.stopwatch.already_exists = Stopwatch '%s' already exists
commands.stopwatch.create.success = Created stopwatch '%s'
commands.stopwatch.does_not_exist = Stopwatch '%s' does not exist
commands.stopwatch.query = Stopwatch '%s' has run for %ss
commands.stopwatch.remove.success = Removed stopwatch '%s'
commands.stopwatch.restart.success = Restarted stopwatch '%s'
```

```
commands.save.alreadyOff = Saving is already turned off
commands.save.alreadyOn = Saving is already turned on
commands.save.disabled = Automatic saving is now disabled
commands.save.enabled = Automatic saving is now enabled
commands.save.failed = Unable to save the game (is there enough disk space?)
commands.save.saving = Saving the game (this may take a moment!)
commands.save.success = Saved the game
```

```
commands.tick.query.percentiles = Percentiles: P50: %sms P95: %sms P99: %sms. Sample: %s
commands.tick.query.rate.running = Target tick rate: %s per second. / Average time per tick: %sms (Target: %sms)
commands.tick.query.rate.sprinting = Target tick rate: %s per second (ignored, reference only). / Average time per tick: %sms
commands.tick.rate.success = Set the target tick rate to %s per second
commands.tick.sprint.report = Sprint completed with %s ticks per second, or %s ms per tick
commands.tick.sprint.stop.fail = No tick sprint in progress
commands.tick.sprint.stop.success = Interrupted the current tick sprint
commands.tick.status.frozen = The game is frozen
commands.tick.status.lagging = The game is running, but can't keep up with the target tick rate
commands.tick.status.running = The game is running normally
commands.tick.status.sprinting = The game is sprinting
commands.tick.step.fail = Unable to step the game - the game must be frozen first
commands.tick.step.stop.fail = No tick step in progress
commands.tick.step.stop.success = Interrupted the current tick step
commands.tick.step.success = Stepping %s tick(s)
```

```
commands.time.no_default_clock = There is no default clock in dimension %s
commands.time.no_time_marker_found = Time marker %s does not exist for clock %s
commands.time.pause = Paused clock %s
commands.time.pause.already_paused = Clock %s is already paused
commands.time.pause.already_running = Clock %s is already running
commands.time.query = The time is %s
commands.time.query.absolute = Clock %s is at %s tick(s)
commands.time.query.gametime = The game time is %s tick(s)
commands.time.query.timeline = Timeline %s is at %s tick(s)
commands.time.query.timeline.repetitions = Timeline %s has passed %s repetition(s)
commands.time.rate = Clock %s will now advance at %sx normal rate
commands.time.rate.already_same = Clock %s is already advancing at %sx normal rate
commands.time.resume = Resumed clock %s
commands.time.set = Set the time to %s
commands.time.set.absolute = Set %s to %s tick(s)
commands.time.set.already_at_time = Clock %s is already set to %s tick(s)
commands.time.set.already_at_time_marker = Clock %s is already at time marker %s
commands.time.set.time_marker = Set %s to time marker %s
commands.time.wrong_timeline_for_clock = Timeline %s is not valid for clock %s
```

```
commands.gamerule.not_set = Game rule %s is already set to %s
commands.gamerule.query = Game rule %s is currently set to %s
commands.gamerule.set = Game rule %s is now set to %s
```

```
commands.difficulty.failure = The difficulty did not change; it is already set to %s
commands.difficulty.query = The difficulty is %s
commands.difficulty.success = The difficulty has been set to %s
```

```
commands.defaultgamemode.success = The default game mode is now %s
```

```
commands.gamemode.success.other = Set %s's game mode to %s
commands.gamemode.success.self = Set own game mode to %s
```

```
commands.teleport.invalidPosition = Invalid position for teleport
commands.teleport.success.entity.multiple = Teleported %s entities to %s
commands.teleport.success.entity.single = Teleported %s to %s
commands.teleport.success.location.multiple = Teleported %s entities to %s, %s, %s
commands.teleport.success.location.single = Teleported %s to %s, %s, %s
```

```
commands.setworldspawn.failure.not_overworld = Can only set the world spawn for the Overworld
commands.setworldspawn.success = Set the world spawn point to %s, %s, %s [%s]
commands.setworldspawn.success.new = Set the world spawn point to %s, %s, %s [%s, %s] in %s
```

```
commands.spawnpoint.success.multiple = Set spawn point to %s, %s, %s [%s] in %s for %s players
commands.spawnpoint.success.multiple.new = Set spawn point to %s, %s, %s [%s, %s] in %s for %s players
commands.spawnpoint.success.single = Set spawn point to %s, %s, %s [%s] in %s for %s
commands.spawnpoint.success.single.new = Set spawn point to %s, %s, %s [%s, %s] in %s for %s
```

```
commands.weather.set.clear = Set the weather to clear
commands.weather.set.rain = Set the weather to rain
commands.weather.set.thunder = Set the weather to rain and thunder
```

## Sondes

Les sondes utilisent le serveur de référence en boîte noire (RCON pour les commandes, `net-probe` de `net-protocol-777-vectors.md` pour les connexions et la capture de paquets), avec une graine fixe et un monde plat. Les comparaisons portent sur la forme, les clés de traduction et leurs arguments (le serveur envoie des composants `translate`) ; les valeurs variables (identifiants d'entité, UUID, horodatages) sont masquées. **Bloquant** : l'EULA du serveur de référence n'est pas acceptée.

| Sonde | Ce qu'elle observe | Procédure | Règles levées |
|---|---|---|---|
| PC1 | Arbre reçu | Cinq joueurs hors ligne `p0` à `p4` dont `ops.json` donne les niveaux 0 à 4 (et un `p5` absent de `ops.json`) : capturer `commands` à la connexion ; décoder tous les nœuds (drapeaux, enfants, redirection, nom, analyseur et propriétés, type de suggestion) ; comparer à `commands.json` (ensemble des chemins, ordre des enfants, indices de nœuds, présence de `publish` et `unpublish`, encodage de `execute run`, drapeau `0x20`, drapeau `0x10` et types de suggestion). Refaire après `op`, `deop` (nouvel envoi ?) et `reload`. | R4, R6, R11, R22, R25, Q4, Q8, Q10 |
| PC2 | Retours d'information | Pour chaque commande de rang A et chaque syntaxe : exécuter par chat avec un joueur de niveau 4, enregistrer le paquet reçu (`system_chat` ou `disguised_chat` : composant NBT complet, clé `translate`, arguments), le résultat via `execute store result score` et le succès via `store success` ; refaire par RCON ; cas d'échec (cible absente, état déjà atteint). | R24, R26 à R30, Q12 |
| PC3 | Analyse | Corpus généré depuis l'arbre : pour chaque chemin exécutable du rang A, une entrée valide, puis des mutations (argument manquant, en trop, type faux, valeur hors bornes, casse, espaces doublés ou finaux, `/` initial, unicode, ligne de 32767 et de 32768 unités) ; enregistrer pour chacune acceptée ou non, clé d'erreur, arguments, position du `[HERE]`, résultat. | R12 à R15, R17, Q1, Q11 |
| PC4 | Commandes d'information | `help` (sans argument et avec chaque nom), `list`, `list uuids`, `version`, `seed` : texte et mise en page exacts, nombre de lignes, ordre, formats numériques. | R26, R27, Q2, Q5 |
| PC5 | Permissions | Chaque joueur de PC1 tente chaque commande racine de rang A et les cinq exigences imbriquées ; un bloc de commandes, une fonction (`function-permission-level` 1 à 4) et la console font de même ; enregistrer le message d'erreur et l'effet. Éditer `ops.json` avec et sans `level` (et avec des noms de niveau) et recharger. | R8 à R10, Q9 |
| PC6 | Retour d'information et journal | `send_command_feedback`, `log_admin_commands`, `command_block_output` à faux puis vrai ; `broadcast-console-to-ops`, `broadcast-rcon-to-ops` ; lire les paquets reçus par un second opérateur et le journal. | R21 |
| PC7 | Sélecteurs | Scène fixe (joueurs, vaches, cochons, armures de support nommées, équipes, scores) ; `@p @r @a @e @s @n` avec chaque option, négations, plages, `limit`, `sort`, doublons, options invalides ; enregistrer les cibles et leur ordre. | R16 |
| PC8 | Suggestions | `command_suggestion` pour un corpus de textes (préfixes de racines, d'arguments de joueurs, de gamemodes, de ressources, texte vide, `/`) ; enregistrer début, longueur, correspondances, info-bulles. | R18 |
| PC9 | Sémantique de rang A | Pour `gamemode`, `defaultgamemode`, `difficulty`, `teleport`, `tp`, `setworldspawn`, `spawnpoint`, `weather` : état avant/après via `/data get`, `execute if`, fichiers sauvegardés, paquets reçus ; coordonnées limites (R17), `~` et `^` mélangés, centrage des `vec3`, durées de météo (échantillon de 200 tirages par type), interaction `advance_weather`, limites `max_*` des gamerules (texte d'erreur). | R17, R23, R29, R30, Q3, Q6 |
| PC10 | Fichiers d'accès | `op`, `deop`, `ban`, `ban-ip`, `pardon`, `pardon-ip`, `whitelist` : contenu des JSON après chaque commande, expulsions et messages de déconnexion des joueurs connectés. Voir aussi PP7 de `server-game-loop.md`. | R28 |

## Annexe : `cmd_ref.py`


```python
"""Reads the command tree report (commands.json) and prints reference tables.

Standard library only. The report root is taken from the REF environment variable
(default .cache/reference/26.3).

Sub-commands:
  table                 one line per root command: level, executable paths, nodes
  usage NAME            every executable path of one root command
  gamerules             the game rules registered under /gamerule (type and bounds)
  parsers               argument parsers used in the tree, with occurrence counts
  levels                root commands grouped by permission
  nested                permission requirements carried by nodes below a root command
  redirects             redirections by target
  lang PREFIX           translation keys (en_us) that start with PREFIX
  registry NAME         protocol ids of a registry (registries.json)
"""
import collections
import json
import os
import sys

REF = os.environ.get("REF", ".cache/reference/26.3")
REPORTS = os.path.join(REF, "generated", "reports")


def tree():
    with open(os.path.join(REPORTS, "commands.json"), encoding="utf-8") as f:
        return json.load(f)


def level_of(node):
    p = node.get("permissions")
    if not p:
        return "all"
    return p["permission"]["level"]


def label(node, name):
    if node["type"] == "literal":
        return name
    parser = node["parser"].replace("minecraft:", "").replace("brigadier:", "")
    return "<%s:%s>" % (name, parser)


def paths(node, name, prefix, out):
    cur = prefix + [label(node, name)] if name else prefix
    if node.get("executable"):
        out.append(" ".join(cur))
    for key, child in node.get("children", {}).items():
        paths(child, key, cur, out)


def count_nodes(node):
    return 1 + sum(count_nodes(c) for c in node.get("children", {}).values())


def cmd_table():
    root = tree()
    print("name | level | executable paths | nodes")
    for name, node in root["children"].items():
        out = []
        paths(node, name, [], out)
        print("%s | %s | %d | %d" % (name, level_of(node), len(out), count_nodes(node)))
    print("total nodes below the root:", sum(count_nodes(n) for n in root["children"].values()))


def cmd_usage(name):
    out = []
    paths(tree()["children"][name], name, [], out)
    print("\n".join(out))


def cmd_gamerules():
    g = tree()["children"]["gamerule"]["children"]
    for key, node in g.items():
        if key.startswith("minecraft:"):
            continue
        value = node["children"]["value"]
        parser = value["parser"].split(":")[1]
        props = value.get("properties", {})
        print("%s | %s | %s" % (key, parser, json.dumps(props, sort_keys=True) if props else ""))
    print("literal children:", len(g), "(each rule appears with and without the minecraft: prefix)")


def cmd_parsers():
    counts = collections.Counter()

    def walk(n):
        if n["type"] == "argument":
            counts[n["parser"]] += 1
        for c in n.get("children", {}).values():
            walk(c)

    walk(tree())
    for p, c in sorted(counts.items()):
        print(c, p)
    print("distinct parsers:", len(counts))


def cmd_levels():
    groups = collections.defaultdict(list)
    for name, node in tree()["children"].items():
        groups[level_of(node)].append(name)
    for k, v in groups.items():
        print(k, len(v), " ".join(v))


def cmd_nested():
    def walk(node, path):
        for key, child in node.get("children", {}).items():
            if "permissions" in child and path:
                print(" ".join(path + [key]), "->", level_of(child))
            walk(child, path + [key])

    walk(tree(), [])


def cmd_redirects():
    counts = collections.Counter()

    def walk(node):
        if "redirect" in node:
            counts["/".join(node["redirect"])] += 1
        for child in node.get("children", {}).values():
            walk(child)

    walk(tree())
    for target, n in counts.most_common():
        print(n, "->", target)


def cmd_lang(prefix):
    with open(os.path.join(REF, "resources", "assets", "minecraft", "lang", "en_us.json"), encoding="utf-8") as f:
        d = json.load(f)
    for k, v in d.items():
        if k.startswith(prefix):
            print(k, "=", v.replace("\n", " / "))


def cmd_registry(name):
    with open(os.path.join(REPORTS, "registries.json"), encoding="utf-8") as f:
        d = json.load(f)
    entries = d[name]["entries"]
    for k, v in sorted(entries.items(), key=lambda kv: kv[1]["protocol_id"]):
        print(v["protocol_id"], k)


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    c = sys.argv[1]
    a = sys.argv[2:]
    fn = globals().get("cmd_" + c)
    if fn is None:
        print(__doc__)
        return 2
    fn(*a)
    return 0


if __name__ == "__main__":
    sys.exit(main())
```


## Annexe : `build_cmds.py`

Produit les deux tableaux de `server-commands.md` (catalogue et analyseurs). La table `M` (rang et jalon de chaque commande) et le dictionnaire `G` (grammaire de chaque analyseur) sont des décisions de l'analyste (S) ; tout le reste vient du rapport.


```python
import collections
import json
import os

rep = os.path.join(os.environ.get('REF', '.cache/reference/26.3'), 'generated', 'reports') + '/'
d = json.load(open(rep + 'commands.json', encoding='utf8'))
reg = json.load(open(rep + 'registries.json', encoding='utf8'))['minecraft:command_argument_type']['entries']
LV = {'gamemasters': 'gamemasters (2)', 'admins': 'admins (3)', 'owners': 'owners (4)', 'all': 'all (0)'}
M = {}


def add(names, tier, own):
    for n in names.split():
        M[n] = (tier, own)


add('help list say me msg tell w tellraw', 'A', 'M8')
add('stop save-all save-off save-on op deop kick ban ban-ip banlist pardon pardon-ip whitelist setidletimeout transfer version seed tick gamerule time difficulty defaultgamemode gamemode tp teleport setworldspawn spawnpoint', 'A', 'M8')
add('weather', 'A', 'M5 (état), M8 (commande)')
add('execute function schedule return reload random tag team teammsg tm trigger scoreboard bossbar title data compute stopwatch datapack', 'B', 'M8 (cadre)')
add('setblock fill clone forceload', 'B', 'M3')
add('fillbiome place locate', 'B', 'M4')
add('worldborder', 'B', 'M5')
add('give clear item loot recipe enchant advancement', 'B', 'M6')
add('summon kill effect attribute damage experience xp spectate spreadplayers ride rotate swing', 'B', 'M7')
add('particle playsound stopsound posteffect dialog waypoint', 'B', 'M9')
add('debug fetchprofile jfr perf publish unpublish test', 'C', 'hors M8')
miss = [n for n in d['children'] if n not in M]
assert not miss, miss
extra = [n for n in M if n not in d['children']]
assert not extra, extra


def execs(n):
    return (1 if n.get('executable') else 0) + sum(execs(c) for c in n.get('children', {}).values())


def nodes(n):
    return 1 + sum(nodes(c) for c in n.get('children', {}).values())


rows = []
for name, n in d['children'].items():
    p = n.get('permissions')
    lv = p['permission']['level'] if p else 'all'
    red = n.get('redirect')
    t, own = M[name]
    rows.append((name, LV[lv], execs(n), nodes(n), 'alias de `%s`' % red[0] if red else '', t, own))
out = ['| Commande | Niveau | Nœuds exécutables | Nœuds | Alias | Rang | Sémantique (jalon) |', '|---|---|---|---|---|---|---|']
for r in rows:
    out.append('| `%s` | %s | %d | %d | %s | %s | %s |' % r)
open('cmd_catalogue.md', 'w', encoding='utf8').write('\n'.join(out) + '\n')
print(collections.Counter(r[5] for r in rows))

G = {
    'brigadier:bool': '`true` ou `false`',
    'brigadier:float': 'flottant 32 bits, bornes facultatives',
    'brigadier:double': 'flottant 64 bits, bornes facultatives',
    'brigadier:integer': 'entier 32 bits, bornes facultatives',
    'brigadier:long': "entier 64 bits (non utilisé par l'arbre)",
    'brigadier:string': '`word` (un mot), `phrase` (citable), `greedy` (reste de la ligne)',
    'minecraft:entity': 'nom de joueur, sélecteur ou UUID ; propriétés `amount` single/multiple et `type` players/entities',
    'minecraft:game_profile': 'nom de joueur ou sélecteur de joueurs',
    'minecraft:block_pos': 'trois entiers ou notation `~` et `^`',
    'minecraft:column_pos': 'deux entiers ou `~` (pas de `^`)',
    'minecraft:vec3': 'trois doubles, `~` et `^`',
    'minecraft:vec2': 'deux doubles, `~` (pas de `^`)',
    'minecraft:block_state': '`id[états]{nbt}`',
    'minecraft:block_predicate': '`id[états]{nbt}` ou `#tag`',
    'minecraft:item_stack': '`id[composants]`',
    'minecraft:item_predicate': '`type[tests]`, type = id, `#tag` ou `*`',
    'minecraft:team_color': 'couleur de texte hors codes hexadécimaux (remplace `color`)',
    'minecraft:hex_color': '3 ou 6 chiffres hexadécimaux',
    'minecraft:component': 'composant de texte (chaîne ou JSON)',
    'minecraft:style': 'objet JSON de style',
    'minecraft:message': 'reste de la ligne, sélecteurs remplacés par les noms',
    'minecraft:nbt_compound_tag': 'SNBT Compound (`core-nbt.md` R39 à R51)',
    'minecraft:nbt_tag': 'SNBT de tout type',
    'minecraft:nbt_path': 'chemin NBT (`a.b[0]."c d"`)',
    'minecraft:objective': 'mot de `- + . _ A-Z a-z 0-9`',
    'minecraft:objective_criteria': 'critère de tableau des scores',
    'minecraft:operation': '`=` `+=` `-=` `*=` `/=` `%=` `><` `<` `>`',
    'minecraft:particle': '`id` ou `id{configuration}`',
    'minecraft:angle': "lacet Float de -180.0 à 179.9, `~` relatif (non utilisé par l'arbre)",
    'minecraft:rotation': 'deux doubles lacet puis tangage (lacet -180 à 179.9, tangage -90 à 90), `~` relatif',
    'minecraft:scoreboard_slot': "emplacement d'affichage",
    'minecraft:score_holder': 'sélecteur, nom, UUID ou `*` ; propriété `amount`',
    'minecraft:swizzle': 'combinaison sans répétition de `x`, `y`, `z`',
    'minecraft:team': 'mot de `- + . _ A-Z a-z 0-9`',
    'minecraft:item_slot': '`type` ou `type.numéro`',
    'minecraft:item_slots': "comme `item_slot` avec `*` (non utilisé par l'arbre)",
    'minecraft:resource_location': "identifiant d'espace de noms optionnel",
    'minecraft:function': 'identifiant de fonction ou `#tag`',
    'minecraft:entity_anchor': '`feet` ou `eyes`',
    'minecraft:int_range': '`N`, `..N`, `N..`, `N..M`',
    'minecraft:float_range': 'idem avec des flottants',
    'minecraft:dimension': 'identifiant de dimension',
    'minecraft:gamemode': '`survival`, `creative`, `adventure`, `spectator`',
    'minecraft:time': 'flottant suivi de `d`, `s` ou `t` ; propriété `min` en ticks',
    'minecraft:resource_or_tag': "identifiant ou `#tag` d'un registre (propriété `registry`)",
    'minecraft:resource_or_tag_key': 'idem, registre rechargeable',
    'minecraft:resource': "identifiant d'un registre (propriété `registry`)",
    'minecraft:resource_key': 'identifiant résolu en clé de registre rechargeable',
    'minecraft:resource_selector': 'identifiant avec jokers `*` et `?`',
    'minecraft:template_mirror': '`none`, `front_back`, `left_right`',
    'minecraft:template_rotation': '`none`, `clockwise_90`, `counterclockwise_90`, `180`',
    'minecraft:heightmap': '`world_surface`, `motion_blocking`, `motion_blocking_no_leaves`, `ocean_floor`',
    'minecraft:loot_table': 'identifiant ou SNBT de table de butin',
    'minecraft:loot_predicate': 'identifiant ou SNBT de prédicat',
    'minecraft:loot_modifier': "identifiant ou SNBT de modificateur d'objet",
    'minecraft:context_float_provider': 'non documenté',
    'minecraft:context_int_provider': 'non documenté',
    'minecraft:slot_source': "identifiant ou SNBT de source d'emplacements",
    'minecraft:dialog': 'identifiant ou SNBT de dialogue',
    'minecraft:feature': 'identifiant ou SNBT de feature',
    'minecraft:swing_animation': '`whack`, `stab`, `none`',
    'minecraft:uuid': 'UUID avec tirets',
}
cnt = collections.Counter()


def w(n):
    if n['type'] == 'argument':
        cnt[n['parser']] += 1
    for c in n.get('children', {}).values():
        w(c)


w(d)
miss = [k for k in reg if k not in G]
assert not miss, miss
out = ['| ID | Analyseur | Occurrences dans l\'arbre | Grammaire (S) |', '|---|---|---|---|']
for k, v in sorted(reg.items(), key=lambda kv: kv[1]['protocol_id']):
    out.append('| %d | `%s` | %d | %s |' % (v['protocol_id'], k, cnt.get(k, 0), G[k]))
open('parser_table.md', 'w', encoding='utf8').write('\n'.join(out) + '\n')
```
