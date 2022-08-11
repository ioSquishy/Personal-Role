# Personal-Role
A discord bot that allows users to create and update their own custom role.

### [Invite me to your server!](https://discord.com/api/oauth2/authorize?client_id=999916712046641222&permissions=268436480&scope=bot%20applications.commands)
## Commands
### /setrole (name) (color)
> This will update your personal role and create one if you don't have one.  
> Enabled for everyone by default.
### /setrequiredrole [role]
> Set a role required for users to use the /setrole command.  
> If the user loses the required role, their personal role will be deleted. (Use Case: Lose role after Nitro boost ends)
> Enabled only for admins by default.
### /updateroles (hoisted) (reset)
> This will move all personal roles under the bots role.
> If hoised is set to true, all personal roles will be hoisted including newly created ones.
> If reset is set to true, all personal roles will be deleted. (Not your other ones).
### /ping
> See if the bot is working and get the invite to my support/feedback server.
