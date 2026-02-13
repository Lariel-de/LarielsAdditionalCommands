# Lariel's Additional Commands

This repository is for the Pixelmon side mod "Lariel's Additional Commands"
using Neo Forge 21.1.200 and Pixelmon's 9.3.14+ API.

<!-- TOC -->
* Pixelmon MDK
  * [Content](#Content)
    * [SpawnNpc-Command](#/larielspawnnpc)
  * [Download](#download)
  * [FAQ](#faq)

## Content
As you can guess from the name of the mod, it adds new Minecraft commands.
I have added to all my additional commands the prefix "lariel" 
to make it easier to find all the additional commands.

### /larielspawnnpc [required some filter]
This command spawns the first npc from your npc presets, that matches the filter. 
This command has autocompletion to make it easier to find the correct preset you're searching for.

#### Example1:
```/larielspawnnpc leader/gym/dragon```

Spawns the first npc that is found in a subfolder of your presets, that matches following conditions:
* the npc is in a subfolder that is in ..leader/gym
* "dragon" could link to an additional subfolder or a .json file.
* 


#### Example2:
```/larielspawnnpc move_tutor_5```

Spawns the first npc that is found in a subfolder of your presets, that matches following conditions:
* "move_tutor_5" could link to a subfolder or a .json file.
  Whatever is found first the command will search there first for a npc preset


#### Example3:
```/larielspawnnpc```

Will print out some error message, because you have to provide at least one key word that could be used as a filter.


### /lariellistpresets [optional: some filter]
Scans the presets folder and lists all available presets. 
You can also provide some optional filter, to find easier the preset you're searching for.

## Download
There is currently no way to download this mod, you sadly have to clone and compile this repo on your own.
I will upload the mod at times to curse forge and will leave a link here.

## FAQ
Frequently Asked Questions

### Can I run this mod in single player?
Yes you can.

### Has this mod to be installed on server and/or client?
To be honest I don't know exactly, I have tested the mod just in single player. 
Since this mod just adds commands that are run spawn entities 
I would say it has to be just installed on server side.