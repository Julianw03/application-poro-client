# Poro-Client
## Project Overview
- 📍[Backend](https://github.com/Julianw03/application-poro-client)
- [Frontend](https://github.com/Julianw03/application-poro-client-frontend)
- [Build Scripts](https://github.com/Julianw03/application-poro-client-build)

## Disclaimer
***Poro-Client** isn't endorsed by Riot Games and doesn't reflect the views or opinions of Riot Games or anyone officially involved in producing or managing Riot Games properties. Riot Games, and all associated properties are trademarks or registered trademarks of Riot Games, Inc.*
THIS IS A PROJECT ONLY PUBLICLY AVAILABLE DURING MY APPLICATION TO RIOT GAMES.
IT IS NOT INTENDED TO BE A COMMERCIALLY USED OR OTHERWISE DISTRIBUTED PRODUCT: ITS A SIDE PROJECT TO IMPROVE MY SKILLS.

## Security
The LCU API allows access to Bearer Tokens used for the shop and general identification and authentication. **This is 
why you should only upload tasks that you have written and compiled yourselves** as they have full access to your client
and therefore your Tokens.
From the outside these tokens cannot be accessed from websites, as the Poro-Client will check the Origin header of the
request that is always send by the browser. If the Origin header does not match the Poro-Client Server, the request will
be denied.
This means that the only way to access the LCU API is by using the Poro-Client Server or by manually setting the Origin
Header, but that means that an application on your computer is trying to access the Data.

## How it works
Basically the League of Legends UI that you see is a web application (Chromium Application with Ember.js) that connects
to the League Client Update API (LCU).  
Once started the Poro-Client will look for the League of Legends UX Process and gather the necessary information to 
connect to the LCU from the process parameters. As soon as we have the necessary information we will connect to the
websocket of the backend as this will notify us of any changes in the client (For example: A user joined your lobby or
a friend came online).  
If you want to learn more how the connection to the LCU is established, you can look at the ConnectionStatemachine