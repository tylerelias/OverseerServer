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

### Starting the Overseer

Easy way: Execute using an IDE (Such as IntelliJ).

Non IDE way:

I made a `start.sh` script that creates the Java classes and runs the server. To run the script, it will require two arguments. An example would be:

`sh start.sh -c 2` where the connection limit is 2. This is a Bash script, with Linux/Mac directory executions and therefor does not work on Windows. If your on Windows just run this in an IDE (Such as IntelliJ) and execute this server from there. Or use other means that you can come up with to run server.

## Making all Threadneedle clients step

You will be asked to insert the amount of steps that you want all clients to take once all clients have connected. 
Just insert a positive number and hit Enter.
You can also make the Threadneedle clients step more once they have completed the steps you instructed them to take before.

### Program arguments

`-c [(int)CLIENT_CONNECTION_LIMIT]` - this command sets the limit of how many Threadneedle clients will be allowed to connect

`-d` - this command makes the server print a lot of information, also tends to make the step input buggy
### In progress

- BankInformation object, trying to reduce the sending of it at the start (tricky, because you can't know for sure if the current list is the actually final list. Fischer's consensus problem and all that)
- (**Implemented, needs testing**) Revert failed transactions (Out of more than 100k steps of testing, it hasn't happened, but it should be a fallback mechanism in the event)

### Bugs / Missing features

- Add argument that saves the log to a special log file
- --help flag with info

Notice: There has no work done on security for this server since it will be communicating with clients within a closed local environment. **Do not run it on an open network**. 
