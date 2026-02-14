# Lariel's Additional Commands

A lightweight Pixelmon side‑mod adding useful commands for server owners, map makers, and single‑player worlds.  
Built for **NeoForge 21.1.200** and **Pixelmon 9.3.14+**.

---

<!-- TOC -->
## Table of Contents
- [Disclaimer](#disclaimer)
- [Features](#features)
- [Commands](#commands)
    - [/larielspawnnpc](#larielspawnnpc)
    - [/lariellistpresets](#lariellistpresets)
    - [/larielsettrainertolevel](#larielsettrainertolevel)
- [Download](#download)
- [FAQ](#faq)
<!-- TOC END -->

---

## Disclaimer

This mod is provided “as is” and is used at your own risk.  
I have developed and tested the mod carefully and have not identified any harmful behavior or issues during testing.  
However, I cannot guarantee that unexpected problems will never occur.

By downloading or using this mod, you acknowledge and agree that the author cannot be held liable for any damages,  
data loss, or other negative effects that may result from installing or using this mod.

---

## Features

Lariel’s Additional Commands adds new Minecraft commands focused on Pixelmon.  
All commands use the prefix **`lariel`** to keep them grouped and easy to find.

Current features:

- Spawn NPCs directly from Pixelmon preset files
  - Autocomplete for preset names
  - Optional search modes (first, last, random)
- List all available presets with optional filtering

---

## Commands

### /larielspawnnpc
Spawns an NPC based on your Pixelmon NPC presets.  
The command supports autocomplete for easier preset discovery.

#### **Syntax**
```/larielspawnnpc <filter> [mode]```
#### **Arguments**

| Argument | Required | Description |
|---------|----------|-------------|
| `<filter>` | Yes | Case‑insensitive substring used to search preset paths (folders + filenames). |
| `[mode]` | No | Determines which preset is selected if multiple matches exist. |

#### **Search Modes**

| Mode | Behavior |
|------|----------|
| `first` | Selects the first matching preset (default). |
| `last` | Selects the last matching preset. |
| `random` | Selects a random matching preset. |

#### **Examples**

**Example 1**  
```/larielspawnnpc "leader/gym/dragon"```  
Spawns the first preset whose path contains `leader/gym/dragon`.

**Example 2**  
```/larielspawnnpc "move_tutor" random```  
Spawns random preset whose path contains `move_tutor`.

**Example 3**  
```/larielspawnnpc "custom_shopkeeper" last```  
Spawns the last preset whose path contains `custom_shopkeeper`.

**Example 4**  
```/larielspawnnpc```  
Shows an error because the filter argument is required.

---

### /lariellistpresets

Scans the Pixelmon preset directory and lists all available presets.  
You can optionally provide a filter to narrow down the results.

#### **Syntax**
```/lariellistpresets [filter]```

#### **Arguments**

| Argument | Required | Description |
|---------|----------|-------------|
| `<filter>` | No       | Case‑insensitive substring used to search preset paths (folders + filenames). |

#### **Examples**

**Example 1**  
```/lariellistpresets```  
Lists all presets.

**Example 2**  
```/lariellistpresets gym```  
Lists only presets containing the word `gym`.

---

### /larielsettrainertolevel
Sets the level of a Pixelmon NPC trainer. The command supports entity selectors and level autocompletion.

#### **Syntax**
```/lariellistpresets [filter]```

**Arguments**

| Argument | Required | Description                                                                |
|---------|----------|----------------------------------------------------------------------------|
| `<target>` | Yes | Any single entity selector (`@e`, UUID). Must be an NPC trainer.           |
| `<level>` | Yes | A numeric level between 1 and 1000.                                        |
| `equal` | Alternative | Sets the NPC's level to the highest level in the executor's Pokémon party. |

**Examples**

**Example 1**  
```/larielsetlevel 085214d4-d952-4b78-afd7-8147436dec57 10```  
Sets NPC trainer with GUID "085214d4-d952-4b78-afd7-8147436dec57" to level 10.

**Example 2**  
```/larielsetlevel @e[type=pixelmon:npc,limit=1,sort=nearest] equal```  
Sets the nearest NPC's level to match the highest level in your party.

---

### /larielpokebattle
Starts a Pokémon battle between two trainers or entities.   
This command is normally provided by Pixelmon, but due to issues in the current version it has been 
temporarily fixed and implemented in this mod (https://pixelmonmod.com/tracker.php?p=2&t=23206). 

#### **Syntax**
```/larielpokebattle <challanger> <opponent1> [opponent2] [opponent3]```

**Arguments**

| Argument       | Required | Description |
|----------------|----------|-------------|
| `<challenger>` | Yes      | The entity that initiates the battle. Can be a player, NPC, or any valid entity selector (`@s`, `@p`, `@e`, UUID, player name). |
| `<opponent1>`  | Yes      | The target entity that will be challenged. Must be a valid battle‑capable entity (player or NPC trainer). |

**Behavior**

- Forces a battle between the two specified entities.
- Both sides must have a valid Pokémon team.
- If either entity is already in a battle, the command will fail with an error message.
- NPC trainers will use their configured teams and AI behavior.
- Players will be prompted with the standard Pixelmon battle UI.

**Notes**

- This command is a temporary fix for issues in the original Pixelmon `/pokebattle` implementation.
- Once Pixelmon resolves the underlying bug, this command will be removed
- Code for this command was rewritten

---

## Download

You can find published releases on the right side of this GitHub repository.  
I will occasionally upload the mod to CurseForge and link it here once available.

---

## FAQ

### **Can I run this mod in single player?**
Yes, absolutely.

### **Does this mod need to be installed on the server, the client, or both?**
This mod has been tested primarily in single‑player.  
Since it only adds commands that spawn entities, it should be sufficient to install it **server‑side**.  
Clients do not need the mod unless I add client‑side features in the future.

---