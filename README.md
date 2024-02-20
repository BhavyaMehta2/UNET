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

Start

**BaseNode startup()**
- Subscribe for DATAGRAM service
- Initialize phy agent for physical layer communication
- Setup WakerBehavior for periodic neighborBroadcast()

**DataNode startup()**
- Initialize rnd for randomization
- Subscribe for DATAGRAM service
- Initialize phy and node agents
- Process messages:
  - If DatagramNtf from another node:
    - Set channelBusy flag
    - Setup WakerBehavior to reset channelBusy after 1500 ms
  - If DatagramNtf with INIT protocol:
    - Respond with acknowledgment (ACK) if channel is not busy
    - If channel is busy, setup WakerBehavior for backoff and CSMA transmission
  - If DatagramNtf with TDMA_INIT protocol:
    - Process TDMA initialization and setup transmission in assigned TDMA slots
    - Setup WakerBehavior for CSMA transmission after TDMA transmission
  - If DatagramNtf with other protocols:
    - Process other protocols (not detailed in flowchart)

**BaseNode processMessage()**
- Process incoming messages:
  - If DatagramNtf with ACK protocol:
    - Add sender to neighbors list and increment counters
  - If DatagramNtf with TDMA protocol:
    - Process TDMA data and increment counters
  - If DatagramNtf with CSMA protocol:
    - Process CSMA data and increment counters
- Parameter access methods (not detailed in flowchart)

End
