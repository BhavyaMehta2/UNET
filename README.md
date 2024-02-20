# Network Simulation Readme

This readme provides an overview of the code and log files related to a network simulation.

## Code Overview

### BaseNode Class

The `BaseNode` class represents a node in a network simulation using the UNet framework. It includes functionalities for neighbor discovery, TDMA broadcasting, and CSMA broadcasting.

#### Functionality Highlights:

- **Neighbor Discovery**: The node periodically broadcasts initialization datagrams to nearby nodes for neighbor discovery.

- **TDMA Broadcasting**: After discovering neighbors, the node broadcasts TDMA time slots to coordinate communication.

- **CSMA Broadcasting**: Based on the number of neighbors, the node starts CSMA broadcasting to manage shared access to the medium.

#### Parameters:

The `BaseNode` class defines the following parameters:

- `tdmaSlotLength`: Length of TDMA time slots.
- `csmaSlotLength`: Length of CSMA time slots.
- `delayLength`: Delay length between actions.
- `neighbours`: List of neighboring nodes.

#### Communication Protocols:

The class defines the following communication protocols:

- `INIT`: Initialization protocol.
- `ACK`: Acknowledgment protocol.
- `TDMA_INIT`: TDMA initialization protocol.
- `TDMA`: TDMA data transmission protocol.
- `CSMA`: CSMA data transmission protocol.

#### Message Processing:

The class includes a method for processing incoming messages, handling acknowledgments, TDMA data, and CSMA data.

#### Parameter Access:

The class provides a method to retrieve a list of all defined parameters.

### DataNode Class

The `DataNode` class represents a data node in a network simulation using the UNet framework. It includes functionalities for processing messages, handling initialization requests, and managing TDMA and CSMA transmissions.

#### Functionality Highlights:

- **Initialization Handling**: The node processes initialization requests from neighboring nodes, acknowledging requests and exchanging protocol parameters.

- **TDMA Transmission**: Upon receiving TDMA initialization, the node starts transmitting data in assigned TDMA slots.

- **CSMA Transmission**: The node initiates CSMA transmission after completing TDMA transmission, managing collisions and backoff periods.

#### Parameters:

The `DataNode` class defines the following parameters:

- `tdmaSlot`: TDMA slot information.
- `csmaSlotLength`: Length of CSMA time slots.
- `tdmaSlotLength`: Length of TDMA time slots.
- `delayLength`: Delay length between actions.

#### Communication Protocols:

The class defines the following communication protocols:

- `INIT`: Initialization protocol.
- `ACK`: Acknowledgment protocol.
- `TDMA_INIT`: TDMA initialization protocol.
- `TDMA`: TDMA data transmission protocol.
- `CSMA`: CSMA data transmission protocol.

#### Message Processing:

The class includes a method for processing incoming messages, handling initialization requests, TDMA initialization, and CSMA transmission.

#### Parameter Access:

The class provides a method to retrieve a list of all defined parameters.

## Log File Overview

The provided log file captures interactions between different nodes in a network simulation. It includes details such as timestamps, node interactions, network operations, node acknowledgments, and more.

[Click here](path/to/logfile.log) to view the log file.


![Description](https://github.com/BhavyaMehta2/UNET/assets/77964425/b7a4bb56-5b0e-4a3f-87d8-3f6faef03251)
