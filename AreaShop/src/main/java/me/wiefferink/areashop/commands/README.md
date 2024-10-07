# Commands

This module contains all AreaShop commands. 
AreaShop uses https://cloud.incendo.org/

* `util/AreashopCommandBean` is extended by all other classes, and implements some general functionality and defines which methods should be implemented.
* Other classes implement specific sub-commands, supply their help messages and tab completion.
* `util/AreashopCommands` class in handles all command registration
