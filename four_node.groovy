import org.arl.unet.addr.AddressResolution
import org.arl.fjage.*
import org.arl.unet.*
import org.arl.unet.phy.*
import org.arl.unet.sim.*
import org.arl.unet.sim.channels.*

///////////////////////////////////////////////////////////////////////////////
// display documentation

println '''
4-node network
--------------

Node A: tcp://localhost:1101, http://localhost:8081/
Node B: tcp://localhost:1102, http://localhost:8082/
Node C: tcp://localhost:1103, http://localhost:8083/
Node D: tcp://localhost:1104, http://localhost:8084/
'''

///////////////////////////////////////////////////////////////////////////////
// simulator configuration
//platform = RealTimePlatform
modem.txDelay = 0 // Set transmission delay of modem to 0

setup1 = { c -> // Define setup1 closure for node configuration
  c.add 'echo', new BaseNode()
  c.add 'arp', new AddressResolution()
}

setup2 = { c -> // Define setup2 closure for node configuration
  c.add 'echo', new DataNode()
  c.add 'arp', new AddressResolution()
}

simulate {
  node 'A', address:1, location: [ 0.km, 0.km, 0.m], web: 8081, api: 1101, stack: setup1 // Create node A with BaseNode stack
  node 'B', address:2, location: [ 0.m, 433.m, -20.m], web: 8082, api: 1102, stack: setup2 // Create node B with DataNode stack
  node 'C', address:3, location: [ -500.m, -433.m, -20.m], web: 8083, api: 1103, stack: setup2 // Create node C with DataNode stack
  node 'D', address:4, location: [ 500.m, -433.m, -20.m], web: 8084, api: 1104, stack: setup2 // Create node D with DataNode stack
}