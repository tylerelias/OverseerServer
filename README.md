# Overseer Server

The Overseer is a server that goes with the [Threadneedle](https://github.com/jackymallett/Threadneedle) economic simulator.
It is a replication og a Real-time gross settlement system, that settle up transactions in real-time.

Its job is to act as a middleman between multiple Threadneedle simulations that will simulate in parallel.
Before the Overseer, Threadneedle was bound to simulating a single machine. With the introduction of the Overseer the size capacity is increased greatly.
The Overseer is responsible for guaranteeing the following:

- Only a set amount of agents *n* can connect to the Overseer at a given time
- The clients can only begin to 'step' once the connection limit has been reached, and all clients have reported as ready to the Overseer
- All connected clients will step in parallel. The Overseer will be in charge of allowing clients to take a step *n+1*
- The Overseer is in charge of sharing the existing Accounts that each Threadneedle client has between other clients, so they know who they can transfer to
- Logging events with a timestamp
- Automatically setting the step limit for all Threadneedle clients that are connected
- The Overseer restricts all Threadneedle clients to step past its set Total Steps
- Clients can make a deposit transaction to another connected client

### Setup Steps

Coming soon, was always tested in an IDE.

### Program arguments

`-s [(int)NUMBER_OF_STEPS_HERE]` - this command sets the number of steps that all Threadneedle clients will be taking in the simulation

`-c [(int)CLIENT_CONNECTION_LIMIT]` - this command sets the limit of how many Threadneedle clients will be allowed to connect

Example argument to run would be:

`java -cp "overseer" Main -s 365 -c 2` (todo: check if this is the right way to run in terminal, spoiled by IDE)

### In progress

- BankInformation object, trying to reduce the sending of it at the start (tricky, because you can't know for sure if the current list is the actually final list. Fischer's consensus problem and all that)
- Revert failed transactions (Out of more than 100k steps of testing, it hasn't happened, but it should be a fallback mechanism in the event)
- Major changes to datastructures were done, all tested a generic simulation test, but they need more looking into for each line of code

### Bugs / Missing features

- Currently, the Overseer stops at the final step *n*, but does not terminate all connections. Not a fatal bug, but an annoying one.
- Add argument that enables debug output (more logs printed) and also argument that saves the log to a special log file
- --help flag with info

Notice: There has no work done on security for this server since it will be communicating with clients within a closed local environment. **Do not run it on an open network**. 
