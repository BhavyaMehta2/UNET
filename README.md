# Network Simulation

This readme provides an overview of the code and log files related to a network simulation.

## Code Overview

### BaseNode Class

The `BaseNode` class, defined in the provided code (`BaseNode.groovy`), represents a node in a network simulation using the UNet framework. It includes functionalities for neighbor discovery, TDMA broadcasting.

#### Functionality Highlights:

- **Neighbor Discovery**: The `BaseNode` periodically broadcasts initialization datagrams to nearby nodes for neighbor discovery.

- **TDMA Broadcasting**: After discovering neighbors, the `BaseNode` broadcasts TDMA time slots to coordinate communication.

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

### DataNode Class

Similarly, the `DataNode` class, defined in the provided code (`DataNode.groovy`), represents a data node in the network simulation. It includes functionalities for processing messages, handling initialization requests, and managing TDMA and CSMA transmissions.

#### Functionality Highlights:

- **Initialization Handling**: The `DataNode` processes initialization requests from neighboring nodes, acknowledging requests and exchanging protocol parameters.

- **TDMA Transmission**: Upon receiving TDMA initialization, the `DataNode` starts transmitting data in assigned TDMA slots.

- **CSMA Transmission**: The `DataNode` initiates CSMA transmission after completing TDMA transmission, managing collisions and backoff periods.

#### Parameters:

The `DataNode` class defines parameters such as `tdmaSlot`, `csmaSlotLength`, `tdmaSlotLength`, and `delayLength`.

#### Communication Protocols:

Similar to `BaseNode`, the `DataNode` class defines communication protocols such as `INIT`, `ACK`, `TDMA_INIT`, `TDMA`, and `CSMA`.

#### Message Processing:

The class includes a method for processing incoming messages, handling initialization requests, TDMA initialization, and CSMA transmission.

## Log File Overview

The provided log file (`simulation.log`) captures interactions between different nodes in the network simulation. It includes details such as timestamps, node interactions, network operations, node acknowledgments, and more.

[Click here](path/to/simulation.log) to view the log file.

```mermaid
graph TD;
    Start((Start)) --> BaseNode;
    BaseNode --> Subscribe;
    BaseNode --> Initialize_phy;
    BaseNode --> Setup_WakerBehavior;
    DataNode --> Initialize_rnd;
    DataNode --> Subscribe;
    DataNode --> Initialize_agents;
    BaseNode --> Process_messages;
    Process_messages[Process messages];
    Process_messages --> |DatagramNtf from another node| ChannelBusy;
    ChannelBusy --> Set_channelBusy_flag;
    ChannelBusy --> Setup_WakerBehavior_reset;
    Set_channelBusy_flag --> |1500ms| Reset_channelBusy_flag;
    Reset_channelBusy_flag --> |if DatagramNtf with INIT protocol| Respond_ACK;
    Respond_ACK --> |If channel is not busy| Send_ACK;
    Respond_ACK --> |If channel is busy| Setup_backoff_and_CSMA;
    Setup_backoff_and_CSMA --> |Backoff and CSMA transmission|;
    Process_messages --> |DatagramNtf with TDMA_INIT protocol| Process_TDMA_init;
    Process_TDMA_init --> Process_TDMA_transmission;
    Process_TDMA_transmission --> Setup_CSMA_transmission;
    Setup_CSMA_transmission --> |After TDMA transmission|;
    Process_messages --> |DatagramNtf with other protocols| Process_other_protocols;
    Process_other_protocols --> |Not detailed| End;
    End((End));

