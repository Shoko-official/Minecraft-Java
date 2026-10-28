# Protocole 777 : vecteurs et sondes

Statut : brouillon
Jalon : M8
Risque : B

Compagnon de `net-protocol-777.md`. Les références Rn renvoient aux règles de cette spec.

## Outil et commandes

Tous les calculs viennent de `net_ref.py` (texte intégral en annexe), Python 3 sans dépendance, sauf `cfb8` qui exige le paquet `cryptography`. Il ne réutilise aucun code du jeu et n'importe rien de `engine/*`. Le copier dans un dossier de scripts hors des données, puis, avec `REF=.cache/reference/26.3` :

```bash
python -I net_ref.py varint      # N01 à N22 (R6 à R8)
python -I net_ref.py frames      # F01 à F16 (R27 à R32, R34, R37, R39)
python -I net_ref.py invalid     # I01 à I12 (R27, R30, R35, R36, R59)
python -I net_ref.py lpvec       # L01 à L05 (R18)
python -I net_ref.py position    # P01 à P06, S01 à S03, B01 (R12, R74)
python -I net_ref.py palette     # K01 à K12 (R24, R69, R70)
python -I net_ref.py hash        # H01 à H04 (R52)
python -I net_ref.py uuid        # U01 à U05 (R58)
python net_ref.py cfb8           # CH01 à CH06 (R31, R50, R51) ; sans -I : le paquet cryptography est hors de l'isolement
python -I net_ref.py commands    # CT01 (R25, R26)
python -I net_ref.py dims        # D01 (R67, R68)
REF=.cache/reference/26.3 python -I net_ref.py ids   # R61
```

Statut des valeurs attendues : les octets de N01 à N21, F01 à F08, F15, F16, L01 à L05, P01 à P06, S01 à S03, B01, K01 à K12, CT01 sont **C** par rapport à des règles qui restent **S** (R6 à R8, R12, R18, R24, R25, R27, R29, R69, R70, R74) : ils sont cohérents avec la documentation publique (les quatre exemples d'octets de LpVec3 et l'exemple de Position `18357644, 831, -20882616` de la documentation sont retrouvés) mais aucun n'a encore été observé sur le jeu. H01 à H03, U01 à U05, CH01 à CH06 sont **C** sur des algorithmes normatifs (SHA-1, MD5, AES-CFB8, RSA PKCS#1 v1.5) ; leur *usage* par le jeu est S (R31, R52, R58). F09 à F14 : le flux zlib de sortie dépend du compresseur (ici le compresseur zlib de Python, niveau par défaut) ; seuls les octets de l'en-tête (longueur, longueur des données) sont attendus à l'identique d'une implémentation à l'autre, et tout flux zlib valide doit se décoder. Les résultats attendus de I01 à I12 sont **provisoires**.


## N : VarInt et VarLong (R6, R7, R8)

```
N01 VarInt  0            00
N02 VarInt  1            01
N03 VarInt  127          7f
N04 VarInt  128          80 01
N05 VarInt  255          ff 01
N06 VarInt  777          89 06
N07 VarInt  25565        dd c7 01
N08 VarInt  2097151      ff ff 7f
N09 VarInt  2097152      80 80 80 01
N10 VarInt  2147483647   ff ff ff ff 07
N11 VarInt  -1           ff ff ff ff 0f
N12 VarInt  -2147483648  80 80 80 80 08
N13 VarLong 0                     00
N14 VarLong 127                   7f
N15 VarLong 128                   80 01
N16 VarLong 9223372036854775807   ff ff ff ff ff ff ff ff 7f
N17 VarLong -1                    ff ff ff ff ff ff ff ff ff 01
N18 VarLong -2147483648           80 80 80 80 f8 ff ff ff ff 01
N19 VarLong -9223372036854775808  80 80 80 80 80 80 80 80 80 01
N20 overlong zero (3 bytes) 80 80 00 -> 0
N21 six-byte VarInt 80 80 80 80 80 80 00 -> VarInt too big
N22 fifth byte 7f, bits beyond 32 dropped: ff ff ff ff 7f -> -1
```

N22 est provisoire (R8) : la documentation dit seulement que le décodage échoue au-delà de 5 octets ; les bits du 5e octet au-delà du 32e bit sont perdus dans une implémentation sur entier de 32 bits. Sonde PN7.


## F : trames (R27 à R32, R34, R37, R39)

```
F01 handshake status    10 00 89 06 09 6c 6f 63 61 6c 68 6f 73 74 63 dd 01
F02 status_request      01 00
F03 ping_request        09 01 01 23 45 67 89 ab cd ef
F04 handshake login     10 00 89 06 09 6c 6f 63 61 6c 68 6f 73 74 63 dd 02
F05 login hello Notch   17 00 05 4e 6f 74 63 68 b5 0a d3 85 82 9d 31 41 a2 16 7e 7d 75 39 ba 7f
F06 login_acknowledged  01 03
F07 login_compression 256 03 03 80 02
F08 finish_configuration (clientbound) 01 03
F09 threshold 256, 10 byte payload id 0x7c: 0c 00 7c 00 01 02 03 04 05 06 07 08 09
F10 threshold 256, 300 x 'A' id 0x7c: 10 ad 02 78 9c ab 71 1c 05 8e c4 02 00 5d 7a 4c a9
    length of frame 17
F11 body 255 bytes (below): 80 02 00 7c 42 42 42 42 42 42 42 42 42 4 ...
F12 body 256 bytes (at threshold) first bytes: 0e 80 02 78 9c ab 71 1a d9 00 00 5d ef 4 ...
F13 threshold 0, empty payload id 0: 0a 01 78 9c 63 00 00 00 01 00 01
F14 compression disabled by threshold -1: same as F02: 01 00
F15 keep_alive config (id 4) long 0x1122334455667788: 09 04 11 22 33 44 55 66 77 88
F16 status_response (id 0) for json {"version":{"name":"26.3","protocol":777}}:
    {"version":{"name":"26.3","protocol":777}} -> 2c 00 2a 7b 22 76 65 72 73 69 6f 6e 22 3a 7b 22 6e 61 6d 65 22 3a 22 32 36 2e 33 22 2c 22 70 72 6f 74 6f 63 6f 6c 22 3a 37 37 37 7d 7d
```

Lecture : F01 = handshake (protocole 777 = `89 06`, adresse `localhost`, port 25565 = `63 dd`, intention 1). F05 = `hello` de login pour `Notch` avec l'UUID hors ligne de U01. F07 : seuil 256 = `80 02`. F09 : dix octets de charge, sous le seuil, donc longueur des données 0. F10 : 301 octets (identifiant + 300) au-dessus du seuil, donc longueur des données `ad 02` (301) puis flux zlib. F11 : corps de 255 octets, sous le seuil : `80 02 00 ...` (longueur de trame 256 = 1 octet de longueur des données + 255, longueur des données 0). F12 : corps de 256 octets, égal au seuil : compressé. F13 : seuil 0, tout est compressé, même une charge vide.


## I : entrées invalides (R27, R30, R35, R36, R59)

```
I01 length field of 4 bytes (value 2097152): 80 80 80 01 ...
I02 length 0 (no packet id): 00
I03 threshold 256, Data Length 0 but 300 byte body (uncompressed above threshold): ad 02 00 7c 41 41 41 41 41 41  ...
I04 Data Length 301 but the zlib stream holds 300 bytes: 10 ad 02 78 9c ab 71 1c 05 8e  ...
I05 Data Length 10 (below threshold 256) with a zlib stream: 0c 0a 78 9c ab 71 82 01 00 10 7c 02 cf
I06 unknown packet id 5 in the handshake state: 01 05
I07 handshake intent 4: 10 00 89 06 09 6c 6f 63 61 6c 68 6f 73 74 63 dd 04
I08 handshake protocol 776, intent 2: 10 00 88 06 09 6c 6f 63 61 6c 68 6f 73 74 63 dd 02
I09 handshake protocol -1, intent 1: 13 00 ff ff ff ff 0f 09 6c 6f 63 61 6c 68 6f 73 74 63 dd 01
I10 login hello with a 17 character name: 23 00 11 41 41 41 41 41 41 41 41 41 41 4 ...
I11 truncated frame: length 9 then 3 bytes: 09 01 02 03
I12 legacy ping first byte: fe 01
```

Résultats attendus (tous provisoires, sonde PN6 ou PA2 selon le cas) :

| Id | Entrée | Résultat provisoire |
|---|---|---|
| I01 | longueur de trame sur 4 octets | erreur de protocole, connexion fermée, aucun octet renvoyé |
| I02 | longueur 0 | idem |
| I03 | seuil 256 ; longueur des données 0 ; corps en clair de 300 octets | accepté (R30 a) |
| I04 | longueur des données 301 ; flux zlib de 300 octets | erreur de protocole (R30 c) |
| I05 | longueur des données 10 (sous le seuil) ; flux zlib | erreur de protocole (R30 b) |
| I06 | identifiant 5 en handshake | connexion fermée sans message |
| I07 | intention 4 | connexion fermée sans message |
| I08 | protocole 776, intention 2 | `login_disconnect` avec la clé `outdated_client` (R36) |
| I09 | protocole -1, intention 1 | réponse de statut normale (R36) |
| I10 | nom de 17 caractères | erreur de protocole (R9), connexion fermée |
| I11 | trame annoncée à 9 octets, 3 reçus | attente jusqu'au délai de lecture, puis fermeture |
| I12 | `fe 01` | connexion fermée sans réponse (R35) |


## L : LpVec3 (R18)

```
L01 LpVec3 (0, 0, 0)                    00                             decode (0.0, 0.0, 0.0) (1 bytes)
L02 LpVec3 (1.0, 0.0, -1.0)             f1 ff 00 00 ff ff              decode (1.0, 0.0, -1.0) (6 bytes)
L03 LpVec3 (10.0, 0.2, -5.0)            f6 ff 40 01 05 1f 02           decode (10.0, 0.2002, -4.9997) (7 bytes)
L04 LpVec3 (123457.0, 15.071, 0.0)      f5 ff 7f ff 00 07 90 f1 01     decode (123457.0, 15.0714, 0.0) (9 bytes)
L05 LpVec3 (0.1, -0.2, 0.3)             29 33 a6 64 cc ca              decode (0.1, -0.2, 0.3) (6 bytes)
    documented bytes 00                             -> (0.0, 0.0, 0.0) (consumed 1 of 1)
    documented bytes f1 ff 00 00 ff ff              -> (1.0, 0.0, -1.0) (consumed 6 of 6)
    documented bytes f6 ff 40 01 05 1f 02           -> (10.0, 0.2, -5.0) (consumed 7 of 7)
    documented bytes f5 ff 7f ff 00 07 90 f1 01     -> (123457.0, 15.071, 0.0) (consumed 9 of 9)
```

L02 à L04 reproduisent les trois vecteurs non nuls de la documentation publique (valeurs d'entrée 1,0 / 0,0 / -1,0 ; 10,0 / 0,2 / -5,0 ; 123457,0 / 15,071 / 0,0). Le décodage rend 0,2002 et -4,9997 pour L03 : la quantification sur 15 bits ne restitue pas exactement 0,2 et -5,0 quand l'échelle est 10 ; c'est attendu, la comparaison se fait sur les octets pour l'encodage et à `1e-3 * s` près pour le décodage. L05 (0,1 / -0,2 / 0,3) n'est pas dans la documentation : S.


## P, S, B : Position, section, bloc (R12, R74)

```
P01 Position (18357644, 831, -20882616)     0x4607632c15b4833f bytes 46 07 63 2c 15 b4 83 3f unpack (18357644, 831, -20882616)
P02 Position (0, 0, 0)                      0x0000000000000000 bytes 00 00 00 00 00 00 00 00 unpack (0, 0, 0)
P03 Position (-1, -1, -1)                   0xffffffffffffffff bytes ff ff ff ff ff ff ff ff unpack (-1, -1, -1)
P04 Position (33554431, 2047, 33554431)     0x7fffffdffffff7ff bytes 7f ff ff df ff ff f7 ff unpack (33554431, 2047, 33554431)
P05 Position (-33554432, -2048, -33554432)  0x8000002000000800 bytes 80 00 00 20 00 00 08 00 unpack (-33554432, -2048, -33554432)
P06 Position (100, 64, -100)                0x0000193ffff9c040 bytes 00 00 19 3f ff f9 c0 40 unpack (100, 64, -100)
S01 SectionPos (0, 0, 0)      0x0000000000000000
S02 SectionPos (-1, -4, -1)   0xfffffffffffffffc
S03 SectionPos (2, 3, -3)     0x00000bffffd00003
B01 block entry state 1, local x=3 z=7 y=5: VarLong f5 26
```

P01 est l'exemple de la documentation publique. S02 : section (-1, -4, -1) -> `ff ff ff ff ff ff ff fc` (x et z à -1 sur 22 bits remplissent tout, y = -4 sur 20 bits).


## K : conteneurs à palette, cartes de hauteur, lumière (R24, R69, R70)

```
K01 blocks, 4096 entries, bits  4: 16 per long,  256 longs,  2048 bytes
K02 blocks, 4096 entries, bits  5: 12 per long,  342 longs,  2736 bytes
K03 blocks, 4096 entries, bits  6: 10 per long,  410 longs,  3280 bytes
K04 blocks, 4096 entries, bits  7:  9 per long,  456 longs,  3648 bytes
K05 blocks, 4096 entries, bits  8:  8 per long,  512 longs,  4096 bytes
K06 blocks, 4096 entries, bits 16:  4 per long, 1024 longs,  8192 bytes
K07 biomes, 64 entries: bits 1 -> 1 long, bits 2 -> 2 longs, bits 3 -> 4 longs, bits 7 -> 8 longs
K06b direct bits: blocks ceil(log2(35723)) = 16 (2^15 = 32768 < 35723), biomes ceil(log2(67)) = 7
K08 heightmap, world height  32: bits 6, 10 per long, 26 longs
K09 heightmap, world height 256: bits 9, 7 per long, 37 longs
K10 heightmap, world height 384: bits 9, 7 per long, 37 longs
K11a entries i%2, bits 4: long0 = 0x1010101010101010 long255 = 0x1010101010101010
K11b entries i%3, bits 5: long0 = 0x0104020804100820 last long (index 341) = 0x0000000000000820
K11c only entry 4095 = 1, bits 4: last long = 0x1000000000000000
K11d single valued block container (air = 0): 00 00
K11e indirect container, palette [0, 1], bits 4: 2052 bytes, first 24: 04 02 00 01 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10
K11f single valued biome container (id 1): 00 01
K12 light masks for 24 sections: 26 bits -> BitSet of 1 long(s)
K12 light masks for 16 sections: 18 bits -> BitSet of 1 long(s)
K12 mask with bits 0 and 25 set (24 sections): BitSet 01 00 00 00 00 02 00 00 01
```

K06 : palier direct des blocs. Le registre compte 35 723 états, plus que 2^15 = 32 768 : `ceil(log2(35723))` = 16 bits, pas 15 ; les 4096 entrées tiennent à 4 par Long, soit 1024 Long (8192 octets). Le palier direct des biomes est `ceil(log2(67))` = 7 (K07). La commande `ids` recompte les états et les biomes dans les données extraites.

K11e : octets d'un conteneur indirect de 4 bits, palette `[0, 1]` : `04` (bits), `02` (taille de palette), `00 01` (palette), puis 256 Long de 8 octets, 2052 octets au total.


## H, U : empreinte de session et UUID hors ligne (R52, R58)

```
H01 signed hex sha1('Notch') = 4ed1f46bbe04bc756bcb17c0c7ce3e4632f06a48
H02 signed hex sha1('jeb_') = -7c9d5b0044c130109a5d7b5fb5c317c02b4e28c1
H03 signed hex sha1('simon') = 88e16a1019277b15d58faf0541e11910eb756f6
H04 serverId '' secret 00..0f, pubkey bytes 00..a1 (162 bytes): sha1 37864836adb7551434f14bef3618e6e0f554897f -> 37864836adb7551434f14bef3618e6e0f554897f
```

```
U01 offline uuid Notch  b50ad385-829d-3141-a216-7e7d7539ba7f
U02 offline uuid jeb_   a762f560-4fce-3236-812a-b80efff0b62b
U03 offline uuid Steve  5627dd98-e6be-3c21-b8a8-e92344183641
U04 offline uuid a      52428a0e-1e30-3cb1-976c-e728b2614047
U05 offline uuid A      b76d4ad3-8d01-3150-b333-68cfb4257774
```

H03 : `simon` donne 39 chiffres hexadécimaux parce que le zéro de tête du résumé (`088e16a...`) disparaît ; une implémentation qui formate avec 40 chiffres échoue sur ce vecteur. H02 est négatif et porte un signe moins. U01 (`Notch`) est la valeur citée par la documentation.


## E : chiffrement (R31, R50, R51)

```
CH01 plaintext frame: 17 00 05 4e 6f 74 63 68 b5 0a d3 85 82 9d 31 41 a2 16 7e 7d 75 39 ba 7f
CH01 ciphertext     : 1d 16 6a dd 3a ef 9a bb 54 7c 35 d3 d2 49 fa d3 5a a2 be 70 f7 52 0f da
CH01 split at byte 5 identical (stream not restarted): True ; manual CFB8 identical: True
CH02 cipher restarted after byte 5 gives other bytes: True
CH03 decrypt round trip: True
CH04 RSA-1024 SubjectPublicKeyInfo DER length: 162 prefix 30 81 9f 30 0d 06 09 2a 86 48 86 f7 0d 01 01 01 05 00 03 81 8d 00
CH05 PKCS#1 v1.5 ciphertext of a 16-byte secret under RSA-1024: 128 bytes
CH06 verify token 4 bytes -> 128 bytes
```

CH01 : clé = vecteur initial = `00 01 ... 0f`. CH02 montre l'erreur classique : redémarrer le chiffreur à chaque paquet. CH04 : préfixe DER `30 81 9f 30 0d 06 09 2a 86 48 86 f7 0d 01 01 01 05 00 03 81 8d 00` d'une clé RSA-1024, 162 octets au total ; la clé du serveur change à chaque démarrage, seul le préfixe et la longueur sont attendus.


## C : arbre de commandes miniature (R25, R26)

```
CT01 node 0: 00 01 01
CT01 node 1: 21 03 02 03 04 04 74 69 63 6b
CT01 node 2: 25 00 06 66 72 65 65 7a 65
CT01 node 3: 25 00 05 71 75 65 72 79
CT01 node 4: 21 01 05 04 72 61 74 65
CT01 node 5: 26 00 04 72 61 74 65 01 03 3f 80 00 00 46 1c 40 00
CT01 commands payload (57 bytes): 06 00 01 01 21 03 02 03 04 04 74 69 63 6b 25 00 06 66 72 65 65 7a 65 25 00 05 71 75 65 72 79 21 01 05 04 72 61 74 65 26 00 04 72 61 74 65 01 03 3f 80 00 00 46 1c 40 00 00
CT01 frame id 0x10: 3a 10 06 00 01 01 21 03 02 03 04 04 74 69 63 6b 25 00 06 66 72 65 65 7a 65 25 00 05 71 75 65 72 79 21 01 05 04 72 61 74 65 26 00 04 72 61 74 65 01 03 3f 80 00 00 46 1c 40 00 00
time parser props min 1: 00 00 00 01
string greedy props: 02
entity single player flags: 0x03
redirect node example: 09 00 00 03 72 75 6e
```

CT01 encode `tick` (restreint) avec trois enfants `freeze`, `query` (exécutables, restreints) et `rate` (restreint) puis l'argument `<rate>` de type flottant borné 1,0 à 10000,0 (`3f 80 00 00` et `46 1c 40 00`), tiré de `commands.json` (D). Le choix des drapeaux "restreint" est une illustration de l'encodage, pas une affirmation sur ce que le jeu envoie : la sonde PX1 de `server-commands.md` donne les vrais. L'ordre des nœuds dans le tableau n'a pas de sens imposé (indice de la racine en dernier champ).


## D : dimensions et sections (R67, R68)

```
D01 sections = height / 16: overworld 384 -> 24, nether and end 256 -> 16
D01 block y -> section index (overworld, min_y -64): y=-64 -> 0, y=-49 -> 0, y=-48 -> 1, y=319 -> 23
```

```
block states: 35723 contiguous from 0: True
direct palette bits: blocks 35723 states -> 16 (constant 35723), biomes 67 -> 7 (constant 67)
blocks in registry order own consecutive state ranges: True blocks: 1286
minecraft:block                       1286 entries, ids contiguous from 0: True
minecraft:item                        1658 entries, ids contiguous from 0: True
minecraft:entity_type                  161 entries, ids contiguous from 0: True
minecraft:data_component_type          122 entries, ids contiguous from 0: True
minecraft:command_argument_type         62 entries, ids contiguous from 0: True
minecraft:game_rule                     59 entries, ids contiguous from 0: True
minecraft:debug_subscription            16 entries, ids contiguous from 0: True
minecraft:menu                          25 entries, ids contiguous from 0: True
minecraft:mob_effect                    40 entries, ids contiguous from 0: True
minecraft:attribute                     40 entries, ids contiguous from 0: True
minecraft:particle_type                128 entries, ids contiguous from 0: True
minecraft:sound_event                 1991 entries, ids contiguous from 0: True
minecraft:game_event                    61 entries, ids contiguous from 0: True
```

## Z : textes de déconnexion (D)

Clés de traduction du fichier `en_us.json` utiles à la connexion (`python -I cmd_ref.py lang multiplayer.disconnect.`). Le serveur envoie la clé (composant `translate`), le client la traduit.


```
multiplayer.disconnect.authservers_down = Authentication servers are down. Please try again later. Sorry!
multiplayer.disconnect.bad_chat_index = Detected a missed or reordered chat message from the server
multiplayer.disconnect.banned = You are banned from this server
multiplayer.disconnect.banned_ip.expiration =  / Your ban will be removed on %s
multiplayer.disconnect.banned_ip.reason = Your IP address is banned from this server. / Reason: %s
multiplayer.disconnect.banned.expiration =  / Your ban will be removed on %s
multiplayer.disconnect.banned.reason = You are banned from this server. / Reason: %s
multiplayer.disconnect.banned.reason.default = Banned by an operator.
multiplayer.disconnect.chat_validation_failed = Chat message validation failure
multiplayer.disconnect.code_of_conduct = Server requires accepting the Code of Conduct
multiplayer.disconnect.configuration_error = Unexpected error during configuration
multiplayer.disconnect.duplicate_login = You logged in from another location
multiplayer.disconnect.expired_public_key = Expired profile public key. Check that your system time is synchronized, and try restarting your game.
multiplayer.disconnect.flying = Flying is not enabled on this server
multiplayer.disconnect.generic = Disconnected
multiplayer.disconnect.idling = You have been idle for too long!
multiplayer.disconnect.illegal_characters = Illegal characters in chat
multiplayer.disconnect.incompatible = Incompatible client! Please use %s
multiplayer.disconnect.invalid_entity_attacked = Attempting to attack an invalid entity
multiplayer.disconnect.invalid_packet = The server sent an invalid packet
multiplayer.disconnect.invalid_player_data = Invalid player data
multiplayer.disconnect.invalid_player_movement = Invalid move player packet received
multiplayer.disconnect.invalid_public_key_signature = Invalid signature for profile public key. / Try restarting your game.
multiplayer.disconnect.invalid_public_key_signature.new = Invalid signature for profile public key. / Try restarting your game.
multiplayer.disconnect.invalid_vehicle_movement = Invalid move vehicle packet received
multiplayer.disconnect.ip_banned = You have been IP banned from this server
multiplayer.disconnect.kicked = Kicked by an operator
multiplayer.disconnect.missing_tags = Incomplete set of tags received from the server. / Please contact a server operator.
multiplayer.disconnect.name_taken = That name is already taken
multiplayer.disconnect.not_whitelisted = You are not white-listed on this server!
multiplayer.disconnect.out_of_order_chat = Out-of-order chat packet received. Did your system time change?
multiplayer.disconnect.outdated_client = Incompatible client! Please use %s
multiplayer.disconnect.outdated_server = Incompatible client! Please use %s
multiplayer.disconnect.server_full = The server is full!
multiplayer.disconnect.server_shutdown = Server closed
multiplayer.disconnect.slow_login = Took too long to log in
multiplayer.disconnect.too_many_pending_chats = Too many unacknowledged chat messages
multiplayer.disconnect.transfers_disabled = This server does not accept transfers
multiplayer.disconnect.unexpected_query_response = Unexpected custom data from client
multiplayer.disconnect.unsigned_chat = Received a chat packet with a missing or invalid signature.
multiplayer.disconnect.unverified_username = Failed to verify username!
```

```
disconnect.endOfStream = End of stream
disconnect.exceeded_packet_rate = Kicked for exceeding packet rate limit
disconnect.genericReason = %s
disconnect.ignoring_status_request = Ignoring status request
disconnect.loginFailedInfo = Failed to log in: %s
disconnect.loginFailedInfo.insufficientPrivileges = Multiplayer is disabled. Please check your Microsoft account settings.
disconnect.loginFailedInfo.invalidSession = Invalid session (Try restarting your game and the launcher)
disconnect.loginFailedInfo.serversUnavailable = The authentication servers are currently not reachable. Please try again.
disconnect.loginFailedInfo.userBanned = You are banned from playing online
disconnect.lost = Connection Lost
disconnect.packetError = Network Protocol Error
disconnect.spam = Kicked for spamming
disconnect.timeout = Timed out
disconnect.transfer = Transferred to another server
disconnect.unknownHost = Unknown host
```

## Sondes

Toutes les sondes utilisent un **client de sonde** (`net-probe`, outil de `tools/oracle` écrit par le `tooling-engineer`) : il ouvre une connexion TCP vers le serveur de référence, écrit les octets littéraux de ce fichier (donc n'utilise ni `engine/*` ni `net/*`), et enregistre dans `reports/probes/<sonde>/` un journal par paquet reçu (instant monotone, état, sens, identifiant, charge brute en hexadécimal) plus le flux TCP brut. Le serveur de référence est lancé en mode sans interface, avec un monde plat à graine fixe, `online-mode=false`, `enforce-secure-profile=false`, `white-list=false`, `enable-rcon=true` (commandes et arrêt propre) et `network-compression-threshold` selon la sonde. **Bloquant** : l'EULA du serveur de référence n'est pas acceptée ; aucune sonde ne peut tourner tant que le propriétaire ne l'a pas fait.

La valeur des champs variables (graine hachée, identifiants d'entité, Long des keep-alive, horloges, UUID) est masquée avant comparaison : le comparateur travaille sur la forme (types, longueurs, ordre) et sur les octets des champs fixes.

### PN1 : NBT réseau (R17 ; `core-nbt.md` R20, R32)

PN1 est la même sonde que PN1 de `core-nbt.md` (trames NBT échangées avec un client vanilla, jalon M8) : l'identifiant est partagé volontairement, une seule capture sert aux deux specs. Les autres identifiants de sonde de ce fichier (PN2 à PN13, PA1 à PA4) et de `server-game-loop.md` (PG, PD) et `server-commands-vectors.md` (PX) ne figurent dans aucune autre spec.

Capturer PN4 et PN5 ; extraire tous les NBT : entrées de `registry_data`, `disconnect`, `system_chat`, `tag_query`. Vérifier : premier octet = type, pas de nom de racine (donc pas de `00 00` après `0a`) ; les composants de texte simples sont des String (octet `08`), les autres des Compound (`0a`) ; une valeur absente est `00`. Mesurer la profondeur et la taille du plus gros NBT du flux (quota côté client non mesurable : on note seulement le maximum observé). Lève R17, R20 de `core-nbt.md` (partiellement R32).

### PN2 : statut (R34, R37, R38)

1. `online-mode=false`, MOTD et favicon absents, aucun joueur : envoyer F01, F02, F03 (puis, dans un autre essai, F01 et F03 seuls, puis F01 F02 F02). Enregistrer les octets de la réponse, vérifier la longueur et le JSON (clés exactes, ordre, présence de `players`, `enforcesSecureChat`).
2. Répéter avec 1 joueur connecté (un second client de sonde en configuration), puis avec `hide-online-players=true`, puis avec `enable-status=false`.
3. Répéter avec `server-ip` vide et un handshake dont l'adresse vaut 255 caractères, le port 0, le protocole -1, puis 776, puis 778.
Lève R34, R36 (status), R37, R38.

### PN3 : login hors ligne et compression (R28 à R30, R39, R57, R58)

Pour chaque seuil dans {-1, 0, 1, 256} : F04 puis `hello` (F05 avec le nom `Notch`, puis `a`, `A`, `jeb_`). Enregistrer : présence et octets de `login_compression`, trame de `login_finished` (UUID attendu U01 à U05, propriétés vides), ordre. Envoyer `login_acknowledged` (F06) et vérifier qu'aucun paquet de configuration n'arrive avant. Pour le seuil 256, envoyer ensuite des paquets de configuration de taille 255, 256 et 257 octets (F11, F12) dans les deux formes (en clair, compressés) et noter les réactions (I03 à I05). Lève R28 à R30, R39, R57, R58.

### PN4 : configuration (R43, R62 à R66, R92, R93)

1. Seuil 256 ; après `login_acknowledged`, envoyer `custom_payload` brand et `client_information` ; enregistrer *tout* jusqu'à `finish_configuration` : ordre, identifiants, taille de chaque `registry_data` (noms de registre, nombre d'entrées, premières entrées en hexadécimal), `update_enabled_features`, présence de `select_known_packs`, `update_tags` (nombre de registres, de tags, octets).
2. Recommencer en répondant à `select_known_packs` par zéro pack, puis par `minecraft:core` avec les versions `26.3`, puis `1.0` ; comparer les `registry_data` (données présentes ou absentes).
3. Recommencer sans envoyer `client_information` ni brand.
4. Comparer les comptes de chaque `registry_data` à ceux de R62 ; comparer le contenu des entrées au JSON de `resources/data/minecraft/**` (types d'étiquettes, R64).
Lève R43, R62 à R66, R92, R93, Q2.

### PN5 : entrée en jeu (R44, R68 à R73, R78, R82, R83, R96)

Après PN4, accepter la configuration (`finish_configuration`), puis enregistrer tout le flux de jeu pendant 600 ticks : ordre exact des 47 étapes de R44, champs de `login`, `commands` (comptes de nœuds), `player_position`, `player_info_update`, `set_time`, `game_event` 13, lots de chunks (taille des lots, intervalle, nombre de chunks avant la première réponse), un chunk complet (octets des sections, nombre de fluides, cartes de hauteur, lumière) et un chunk dense préparé avant la connexion : une section de 256 états de blocs distincts, une de 257 (un état distinct par `setblock`, un seul bloc par état), une section de 8 biomes distincts et une de 9 (`fillbiome`). Relever l'octet de bits par entrée de chaque conteneur, la présence d'une palette et la taille du tableau de Long. Attendu provisoire (R69) : 8 puis 16 pour les blocs, 3 puis 7 pour les biomes, aucune valeur de 9 à 15 pour les blocs. Répondre `accept_teleportation`, `chunk_batch_received` (valeurs 1, 9, 100), `player_loaded`. Refaire avec `client_tick_end` absent, avec `player_loaded` absent. Lève R44, R68 à R73, R78, R82, R83, R96, Q1, Q3, Q11, Q12.

### PN6 : entrées invalides de transport et d'état (R27, R30, R35, R36, R49, R85)

Envoyer I01 à I12 dans chaque état possible (handshake, status, login, configuration, play), ainsi que des paquets d'un état dans un autre, un identifiant hors table (255) et une trame de 2^21 - 1 octets. Noter pour chacun : fermeture de la connexion ou non, délai, présence d'un paquet de déconnexion et sa clé de traduction, journal du serveur. Lève R27, R30, R35, R36, R49, Q7.

### PN7 : types invalides (R5, R8, R9, R10)

En configuration : `client_information` avec un Boolean valant `02`, une langue de 17 caractères, une String en UTF-8 invalide (`ff`), une longueur négative, un Identifier invalide (majuscule) dans `custom_payload`, un VarInt de 6 octets, N22. Noter la réaction. Lève R5, R8, R9, R10.

### PN8 : keep-alive (R47)

En configuration et en jeu, ne jamais répondre aux keep-alive ; noter l'intervalle entre deux émissions et l'instant de la déconnexion et sa clé. Répondre avec une mauvaise valeur. Lève R47, Q5.

### PN9 : mode en ligne, demande de chiffrement (R50, R51)

`online-mode=true` : envoyer F04 puis `hello` ; enregistrer `hello` clientbound : longueur de la clé, préfixe DER (CH04), longueur du jeton, `should_authenticate`, `server_id`. Ne pas répondre. Le reste du flux (R52 à R55) n'est vérifiable que par le scénario L4 en ligne avec le compte du propriétaire sur un serveur local, car l'API de sessions de Mojang ne peut pas être simulée sans configurer le serveur de référence autrement que par défaut. Lève R50 (partiellement).

### PN10 : ping hérité (R35)

Envoyer `fe 01` seul, puis `fe 01 fa` suivi du corps du ping 1.6 de la documentation. Noter si le serveur répond (paquet `ff`, UTF-16) ou ferme. Lève R35, Q8.

### PN11 : transfert (R46)

Handshake avec intention 3 pour `accepts-transfers` faux puis vrai. Noter la déconnexion et la clé. Lève R46.

### PN12 : mouvement, blocs, conteneurs (R75, R76, Q6, Q9)

Après PN5 : envoyer `move_player_pos` pour des déplacements de 0,1 ; 1 ; 10 ; 100 blocs, `NaN`, une position hors bordure ; `player_action` début et fin de minage d'un bloc proche et lointain avec séquences croissantes, `use_item_on` sur une face valide et invalide, `set_creative_mode_slot` ; noter les réponses (`block_update`, `block_changed_ack`, `player_position`, expulsion). Lève R75, R76, Q6, Q9.

### PN13 : chat, commandes, reconfiguration (R43, R45, R79)

Envoyer `chat` (non signé), `chat_command` (`time query gametime`), `chat_command_signed` sans signature valide, `chat_session_update` avec une clé quelconque, `command_suggestion` (`/ti`). Noter les paquets reçus. Si une commande ou un réglage du serveur de référence déclenche `start_configuration`, répondre `configuration_acknowledged` et enregistrer la séquence. Lève R79, R80, R45, Q4.

### PA1 à PA4 : authentification et accès (R58 à R60, R40)

- **PA1** : UUID hors ligne pour {`Notch`, `jeb_`, `Steve`, `a`, `A`, `Ünïcode`, un nom de 16 caractères} (comparer à U01 à U05 et à MD5 pour les autres) ; vérifier aussi que l'UUID envoyé par le client dans `hello` est ignoré.
- **PA2** : noms limites : vide, 1 caractère, 3, 16, 17 (I10), avec espace, avec `§`, avec tiret, point, caractère de contrôle ; noter acceptation ou rejet et message.
- **PA3** : même nom connecté deux fois (même UUID), liste blanche active sans le joueur, joueur banni (`ban`), IP bannie, serveur plein (`max-players=1`) avec et sans `bypassesPlayerLimit` ; noter message, moment (avant ou après `login_finished`), ordre des contrôles quand plusieurs s'appliquent.
- **PA4** : délai de connexion : ouvrir la connexion et ne rien envoyer ; envoyer le handshake de login seul ; envoyer `hello` puis ne pas envoyer `login_acknowledged` ; mesurer chaque délai de fermeture et noter la clé.
Lèvent R58 à R60, R40, Q5.

## Annexe : `net_ref.py`


```python
"""Reference calculations for net-protocol-777 (independent of the game and of engine/*).

Standard library; the cfb8 sub-command also needs the 'cryptography' package.
Sub-commands: varint, frames, invalid, lpvec, position, palette, hash, uuid, cfb8,
commands, dims, ids. Only cfb8 needs the cryptography package; ids needs REF.
"""
import hashlib
import json
import math
import struct
import sys
import zlib


def venc(v, bits=32):
    v &= (1 << bits) - 1
    out = bytearray()
    while v & ~0x7F:
        out.append((v & 0x7F) | 0x80)
        v >>= 7
    out.append(v)
    return bytes(out)


def vdec(b, pos=0, bits=32):
    r = 0
    shift = 0
    while True:
        if shift >= bits:
            raise ValueError("VarInt too big")
        c = b[pos]
        pos += 1
        r |= (c & 0x7F) << shift
        shift += 7
        if not c & 0x80:
            break
    r &= (1 << bits) - 1
    if r >= 1 << (bits - 1):
        r -= 1 << bits
    return r, pos


def string(s):
    e = s.encode("utf-8")
    return venc(len(e)) + e


def frame(pid, payload, threshold=None):
    body = venc(pid) + payload
    if threshold is None:
        return venc(len(body)) + body
    if len(body) >= threshold:
        z = zlib.compress(body)
        inner = venc(len(body)) + z
    else:
        inner = venc(0) + body
    return venc(len(inner)) + inner


def hx(b):
    return " ".join("%02x" % x for x in b)


def cmd_varint():
    n = 1
    for v in (0, 1, 127, 128, 255, 777, 25565, 2097151, 2097152, 2147483647, -1, -2147483648):
        print("N%02d VarInt  %-12d %s" % (n, v, hx(venc(v))))
        n += 1
    for v in (0, 127, 128, 9223372036854775807, -1, -2147483648, -9223372036854775808):
        print("N%02d VarLong %-21d %s" % (n, v, hx(venc(v, 64))))
        n += 1
    assert vdec(bytes.fromhex("ffffffff0f"))[0] == -1
    assert vdec(bytes.fromhex("8080808008"))[0] == -2147483648
    print("N20 overlong zero (3 bytes) 80 80 00 ->", vdec(bytes.fromhex("808000"))[0])
    try:
        vdec(bytes.fromhex("80808080808000"))
    except ValueError as e:
        print("N21 six-byte VarInt 80 80 80 80 80 80 00 ->", e)
    print("N22 fifth byte 7f, bits beyond 32 dropped: ff ff ff ff 7f ->", vdec(bytes.fromhex("ffffffff7f"))[0])


def cmd_frames():
    hs = venc(777) + string("localhost") + struct.pack(">H", 25565) + venc(1)
    print("F01 handshake status   ", hx(frame(0, hs)))
    print("F02 status_request     ", hx(frame(0, b"")))
    print("F03 ping_request       ", hx(frame(1, struct.pack(">q", 0x0123456789ABCDEF))))
    hs2 = venc(777) + string("localhost") + struct.pack(">H", 25565) + venc(2)
    print("F04 handshake login    ", hx(frame(0, hs2)))
    uid = offline_uuid("Notch")
    print("F05 login hello Notch  ", hx(frame(0, string("Notch") + uid.bytes)))
    print("F06 login_acknowledged ", hx(frame(3, b"")))
    print("F07 login_compression 256", hx(frame(3, venc(256))))
    print("F08 finish_configuration (clientbound)", hx(frame(3, b"")))
    small = bytes(range(10))
    print("F09 threshold 256, 10 byte payload id 0x7c:", hx(frame(0x7C, small, 256)))
    big = b"A" * 300
    f = frame(0x7C, big, 256)
    print("F10 threshold 256, 300 x 'A' id 0x7c:", hx(f))
    print("    length of frame", len(f))
    body255 = b"B" * 254  # id (1 byte) + 254 = 255 < 256
    print("F11 body 255 bytes (below):", hx(frame(0x7C, body255, 256))[:40], "...")
    body256 = b"B" * 255
    f2 = frame(0x7C, body256, 256)
    print("F12 body 256 bytes (at threshold) first bytes:", hx(f2)[:40], "...")
    print("F13 threshold 0, empty payload id 0:", hx(frame(0, b"", 0)))
    print("F14 compression disabled by threshold -1: same as F02:", hx(frame(0, b"")))
    print("F15 keep_alive config (id 4) long 0x1122334455667788:", hx(frame(4, struct.pack(">q", 0x1122334455667788))))
    print("F16 status_response (id 0) for json {\"version\":{\"name\":\"26.3\",\"protocol\":777}}:")
    js = json.dumps({"version": {"name": "26.3", "protocol": 777}}, separators=(",", ":"))
    print("   ", js, "->", hx(frame(0, string(js))))


def lp_encode(x, y, z):
    m = max(abs(x), abs(y), abs(z))
    if m < 1 / 32766:
        return b"\x00"
    s = math.ceil(m)

    def q(v):
        return int(round((v / s * 0.5 + 0.5) * 32766))

    lo = s & 3
    cont = s > 3
    packed = lo | (4 if cont else 0) | (q(x) << 3) | (q(y) << 18) | (q(z) << 33)
    out = bytearray()
    out.append(packed & 0xFF)
    out.append((packed >> 8) & 0xFF)
    out += struct.pack(">I", (packed >> 16) & 0xFFFFFFFF)
    if cont:
        out += venc(s >> 2)
    return bytes(out)


def lp_decode(b):
    if b[0] == 0:
        return (0.0, 0.0, 0.0), 1
    a, c = b[0], b[1]
    hi = struct.unpack(">I", b[2:6])[0]
    packed = a | (c << 8) | (hi << 16)
    s = packed & 3
    pos = 6
    if packed & 4:
        v, pos = vdec(b, pos)
        s |= v << 2

    def u(v):
        return min(v, 32766) * 2 / 32766 - 1

    return (u((packed >> 3) & 0x7FFF) * s, u((packed >> 18) & 0x7FFF) * s, u((packed >> 33) & 0x7FFF) * s), pos


def cmd_lpvec():
    k = 1
    for v in ((0, 0, 0), (1.0, 0.0, -1.0), (10.0, 0.2, -5.0), (123457.0, 15.071, 0.0), (0.1, -0.2, 0.3)):
        e = lp_encode(*v)
        d, n = lp_decode(e)
        print("L%02d LpVec3 %-28s %-30s decode %s (%d bytes)" % (k, v, hx(e), tuple(round(t, 4) for t in d), n))
        k += 1
    # the four byte strings given by the public documentation (L01 to L04 encode to the same bytes)
    for s in ("00", "f1 ff 00 00 ff ff", "f6 ff 40 01 05 1f 02", "f5 ff 7f ff 00 07 90 f1 01"):
        b = bytes.fromhex(s.replace(" ", ""))
        d, n = lp_decode(b)
        print("    documented bytes %-30s -> %s (consumed %d of %d)" % (s, tuple(round(t, 3) for t in d), n, len(b)))


def pack_pos(x, y, z):
    return ((x & 0x3FFFFFF) << 38) | ((z & 0x3FFFFFF) << 12) | (y & 0xFFF)


def unpack_pos(v):
    def sx(n, bits):
        return n - (1 << bits) if n >= 1 << (bits - 1) else n

    return sx(v >> 38, 26), sx(v & 0xFFF, 12), sx((v >> 12) & 0x3FFFFFF, 26)


def cmd_position():
    k = 1
    for p in ((18357644, 831, -20882616), (0, 0, 0), (-1, -1, -1), (33554431, 2047, 33554431), (-33554432, -2048, -33554432), (100, 64, -100)):
        v = pack_pos(*p)
        print("P%02d Position %-30s 0x%016x bytes %s unpack %s" % (k, p, v, hx(struct.pack(">Q", v)), unpack_pos(v)))
        k += 1
    # section position for Update Section Blocks: x 22 bits, y 20 bits, z 22 bits (shifts 42, 0, 20)
    def sec(x, y, z):
        return ((x & 0x3FFFFF) << 42) | (y & 0xFFFFF) | ((z & 0x3FFFFF) << 20)

    k = 1
    for p in ((0, 0, 0), (-1, -4, -1), (2, 3, -3)):
        print("S%02d SectionPos %-14s 0x%016x" % (k, p, sec(*p)))
        k += 1
    # block entry in section update: stateId << 12 | x << 8 | z << 4 | y
    print("B01 block entry state 1, local x=3 z=7 y=5: VarLong", hx(venc((1 << 12) | (3 << 8) | (7 << 4) | 5, 64)))


def pack_longs(entries, bpe):
    per = 64 // bpe
    n = -(-len(entries) // per)
    longs = [0] * n
    for i, e in enumerate(entries):
        longs[i // per] |= e << ((i % per) * bpe)
    return longs


BLOCK_STATES = 35723  # R61, checked against blocks.json by the ids sub-command
BIOMES = 67  # R62, checked against the datapack files by the ids sub-command


def direct_bits(registry_size):
    return (registry_size - 1).bit_length()  # ceil(log2(size))


def cmd_palette():
    k = 1
    direct_blocks = direct_bits(BLOCK_STATES)
    direct_biomes = direct_bits(BIOMES)
    for bpe in (4, 5, 6, 7, 8, direct_blocks):
        per = 64 // bpe
        print("K%02d blocks, 4096 entries, bits %2d: %2d per long, %4d longs, %5d bytes" % (k, bpe, per, -(-4096 // per), -(-4096 // per) * 8))
        k += 1
    per = 64 // direct_biomes
    print("K%02d biomes, 64 entries: bits 1 -> %d long, bits 2 -> %d longs, bits 3 -> %d longs, bits %d -> %d longs" % (k, -(-64 // 64), -(-64 // 32), -(-64 // 21), direct_biomes, -(-64 // per)))
    k += 1
    print("K06b direct bits: blocks ceil(log2(%d)) = %d (2^15 = %d < %d), biomes ceil(log2(%d)) = %d" % (BLOCK_STATES, direct_blocks, 1 << 15, BLOCK_STATES, BIOMES, direct_biomes))
    for h in (32, 256, 384):
        bpe = math.ceil(math.log2(h + 1))
        per = 64 // bpe
        print("K%02d heightmap, world height %3d: bits %d, %d per long, %d longs" % (k, h, bpe, per, -(-256 // per)))
        k += 1
    ents = [i % 2 for i in range(4096)]
    l = pack_longs(ents, 4)
    print("K11a entries i%%2, bits 4: long0 = 0x%016x long255 = 0x%016x" % (l[0], l[255]))
    ents = [i % 3 for i in range(4096)]
    l = pack_longs(ents, 5)
    print("K11b entries i%%3, bits 5: long0 = 0x%016x last long (index %d) = 0x%016x" % (l[0], len(l) - 1, l[-1]))
    ents = [0] * 4096
    ents[4095] = 1
    l = pack_longs(ents, 4)
    print("K11c only entry 4095 = 1, bits 4: last long = 0x%016x" % l[-1])
    print("K11d single valued block container (air = 0):", hx(bytes([0]) + venc(0)))
    pal = bytes([4]) + venc(2) + venc(0) + venc(1) + b"".join(struct.pack(">Q", x) for x in pack_longs([i % 2 for i in range(4096)], 4))
    print("K11e indirect container, palette [0, 1], bits 4: %d bytes, first 24: %s" % (len(pal), hx(pal[:24])))
    print("K11f single valued biome container (id 1):", hx(bytes([0]) + venc(1)))
    for sections in (24, 16):
        print("K12 light masks for %d sections: %d bits -> BitSet of %d long(s)" % (sections, sections + 2, -(-(sections + 2) // 64)))
    m = (1 << 0) | (1 << 25)
    print("K12 mask with bits 0 and 25 set (24 sections): BitSet", hx(venc(1) + struct.pack(">Q", m)))


def java_hex(d):
    n = int.from_bytes(d, "big", signed=True)
    return format(n, "x") if n >= 0 else "-" + format(-n, "x")


def cmd_hash():
    k = 1
    for n in ("Notch", "jeb_", "simon"):
        d = hashlib.sha1(n.encode()).digest()
        print("H%02d signed hex sha1(%r) = %s" % (k, n, java_hex(d)))
        k += 1
    secret = bytes(range(16))
    pub = bytes(range(162))
    h = hashlib.sha1(b"" + secret + pub).digest()
    print("H04 serverId '' secret 00..0f, pubkey bytes 00..a1 (162 bytes): sha1 %s -> %s" % (h.hex(), java_hex(h)))


def offline_uuid(name):
    d = bytearray(hashlib.md5(("OfflinePlayer:" + name).encode("utf-8")).digest())
    d[6] = (d[6] & 0x0F) | 0x30
    d[8] = (d[8] & 0x3F) | 0x80
    import uuid

    return uuid.UUID(bytes=bytes(d))


def cmd_uuid():
    k = 1
    for n in ("Notch", "jeb_", "Steve", "a", "A"):
        print("U%02d offline uuid %-6s %s" % (k, n, offline_uuid(n)))
        k += 1


def cfb8_manual(key, iv, data, decrypt=False):
    from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

    ecb = Cipher(algorithms.AES(key), modes.ECB()).encryptor()
    reg = bytearray(iv)
    out = bytearray()
    for b in data:
        k = ecb.update(bytes(reg))[0]
        o = b ^ k
        out.append(o)
        c = b if decrypt else o
        reg = reg[1:] + bytes([c])
    return bytes(out)


def cmd_cfb8():
    from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
    from cryptography.hazmat.primitives import serialization
    from cryptography.hazmat.primitives.asymmetric import rsa

    try:
        from cryptography.hazmat.decrepit.ciphers.modes import CFB8
    except ImportError:
        CFB8 = modes.CFB8

    key = bytes(range(16))
    plain = frame(0, string("Notch") + offline_uuid("Notch").bytes)
    enc = Cipher(algorithms.AES(key), CFB8(key)).encryptor()
    whole = enc.update(plain) + enc.finalize()
    enc2 = Cipher(algorithms.AES(key), CFB8(key)).encryptor()
    split = enc2.update(plain[:5]) + enc2.update(plain[5:]) + enc2.finalize()
    man = cfb8_manual(key, key, plain)
    print("CH01 plaintext frame:", hx(plain))
    print("CH01 ciphertext     :", hx(whole))
    print("CH01 split at byte 5 identical (stream not restarted):", whole == split, "; manual CFB8 identical:", whole == man)
    restarted = Cipher(algorithms.AES(key), CFB8(key)).encryptor()
    wrong = restarted.update(plain[:5]) + Cipher(algorithms.AES(key), CFB8(key)).encryptor().update(plain[5:])
    print("CH02 cipher restarted after byte 5 gives other bytes:", wrong != whole)
    dec = Cipher(algorithms.AES(key), CFB8(key)).decryptor()
    print("CH03 decrypt round trip:", dec.update(whole) == plain)
    k = rsa.generate_private_key(public_exponent=65537, key_size=1024)
    der = k.public_key().public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)
    print("CH04 RSA-1024 SubjectPublicKeyInfo DER length:", len(der), "prefix", hx(der[:22]))
    from cryptography.hazmat.primitives.asymmetric import padding

    ct = k.public_key().encrypt(key, padding.PKCS1v15())
    print("CH05 PKCS#1 v1.5 ciphertext of a 16-byte secret under RSA-1024:", len(ct), "bytes")
    token = bytes([1, 2, 3, 4])
    print("CH06 verify token 4 bytes ->", len(k.public_key().encrypt(token, padding.PKCS1v15())), "bytes")


def node(flags, children, redirect=None, name=None, parser=None, props=b""):
    out = bytearray([flags]) + venc(len(children)) + b"".join(venc(c) for c in children)
    if flags & 0x08:
        out += venc(redirect)
    if name is not None:
        out += string(name)
    if parser is not None:
        out += venc(parser) + props
    return bytes(out)


def cmd_commands():
    # Miniature tree: root -> tick -> {freeze, query, rate <rate:float 1.0..10000.0>}
    # flags: type (0x03) 1 literal 2 argument, 0x04 executable, 0x20 restricted
    nodes = [
        node(0x00, [1]),
        node(0x21, [2, 3, 4], name="tick"),
        node(0x25, [], name="freeze"),
        node(0x25, [], name="query"),
        node(0x21, [5], name="rate"),
        node(0x26, [], name="rate", parser=1, props=bytes([0x03]) + struct.pack(">f", 1.0) + struct.pack(">f", 10000.0)),
    ]
    payload = venc(len(nodes)) + b"".join(nodes) + venc(0)
    for i, n in enumerate(nodes):
        print("CT01 node %d: %s" % (i, hx(n)))
    print("CT01 commands payload (%d bytes): %s" % (len(payload), hx(payload)))
    print("CT01 frame id 0x10:", hx(frame(0x10, payload)))
    # time argument: Int minimum in ticks
    print("time parser props min 1:", hx(struct.pack(">i", 1)))
    # string parser: greedy
    print("string greedy props:", hx(venc(2)))
    # entity parser: single player
    print("entity single player flags: 0x03")
    # redirect example (execute run -> root): flags literal(1) | redirect 0x08 = 0x09
    print("redirect node example:", hx(node(0x09, [], redirect=0, name="run")))


def cmd_dims():
    print("D01 sections = height / 16: overworld 384 -> %d, nether and end 256 -> %d" % (384 // 16, 256 // 16))
    print("D01 block y -> section index (overworld, min_y -64): y=-64 -> %d, y=-49 -> %d, y=-48 -> %d, y=319 -> %d" % ((-64 + 64) // 16, (-49 + 64) // 16, (-48 + 64) // 16, (319 + 64) // 16))


def cmd_ids():
    import os

    ref = os.environ.get("REF", ".cache/reference/26.3")
    reports = os.path.join(ref, "generated", "reports")
    with open(os.path.join(reports, "blocks.json"), encoding="utf-8") as f:
        blocks = json.load(f)
    with open(os.path.join(reports, "registries.json"), encoding="utf-8") as f:
        reg = json.load(f)
    ids = sorted(s["id"] for v in blocks.values() for s in v["states"])
    print("block states:", len(ids), "contiguous from 0:", ids == list(range(len(ids))))
    biome_dir = os.path.join(ref, "resources", "data", "minecraft", "worldgen", "biome")
    n_biomes = sum(1 for _, _, fs in os.walk(biome_dir) for f in fs if f.endswith(".json"))
    print("direct palette bits: blocks %d states -> %d (constant %d), biomes %d -> %d (constant %d)" % (
        len(ids), direct_bits(len(ids)), BLOCK_STATES, n_biomes, direct_bits(n_biomes), BIOMES))
    assert (len(ids), n_biomes) == (BLOCK_STATES, BIOMES)
    order = sorted(reg["minecraft:block"]["entries"].items(), key=lambda kv: kv[1]["protocol_id"])
    prev = -1
    ok = True
    for name, _ in order:
        first = min(s["id"] for s in blocks[name]["states"])
        ok &= first > prev
        prev = max(s["id"] for s in blocks[name]["states"])
    print("blocks in registry order own consecutive state ranges:", ok, "blocks:", len(order))
    for key in ("minecraft:block", "minecraft:item", "minecraft:entity_type", "minecraft:data_component_type", "minecraft:command_argument_type", "minecraft:game_rule", "minecraft:debug_subscription", "minecraft:menu", "minecraft:mob_effect", "minecraft:attribute", "minecraft:particle_type", "minecraft:sound_event", "minecraft:game_event"):
        e = reg[key]["entries"]
        pids = sorted(v["protocol_id"] for v in e.values())
        print("%-36s %5d entries, ids contiguous from 0: %s" % (key, len(e), pids == list(range(len(e)))))


def cmd_invalid():
    thr = 256
    body = bytes([0x7C]) + b"A" * 299  # id + 299 bytes = 300 bytes
    print("I01 length field of 4 bytes (value 2097152):", hx(bytes.fromhex("80808001")) + " ...")
    print("I02 length 0 (no packet id):", hx(bytes([0])))
    print("I03 threshold 256, Data Length 0 but 300 byte body (uncompressed above threshold):", hx(venc(1 + len(body)) + bytes([0]) + body)[:30], "...")
    z = zlib.compress(body)
    print("I04 Data Length 301 but the zlib stream holds 300 bytes:", hx(venc(len(venc(301)) + len(z)) + venc(301) + z)[:30], "...")
    small = bytes([0x7C]) + b"B" * 9
    zs = zlib.compress(small)
    print("I05 Data Length 10 (below threshold 256) with a zlib stream:", hx(venc(len(venc(10)) + len(zs)) + venc(10) + zs))
    print("I06 unknown packet id 5 in the handshake state:", hx(frame(5, b"")))
    print("I07 handshake intent 4:", hx(frame(0, venc(777) + string("localhost") + struct.pack(">H", 25565) + venc(4))))
    print("I08 handshake protocol 776, intent 2:", hx(frame(0, venc(776) + string("localhost") + struct.pack(">H", 25565) + venc(2))))
    print("I09 handshake protocol -1, intent 1:", hx(frame(0, venc(-1) + string("localhost") + struct.pack(">H", 25565) + venc(1))))
    print("I10 login hello with a 17 character name:", hx(frame(0, string("A" * 17) + offline_uuid("A" * 17).bytes))[:40], "...")
    print("I11 truncated frame: length 9 then 3 bytes:", hx(bytes([9, 1, 2, 3])))
    print("I12 legacy ping first byte:", hx(bytes([0xFE, 0x01])))


def main():
    cmds = {k[4:]: v for k, v in globals().items() if k.startswith("cmd_")}
    if len(sys.argv) < 2 or sys.argv[1] not in cmds:
        print("usage: net_ref.py", "|".join(cmds))
        return 2
    cmds[sys.argv[1]]()
    return 0


if __name__ == "__main__":
    sys.exit(main())
```
