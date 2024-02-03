import org.arl.fjage.*;
import org.arl.unet.*
import org.arl.unet.phy.*

def attempts = 2      // try only a single attempt at discovery
def phantom = 132   // non-existent node address
def timeout = 10000   // 5 second timeout
  
println 'Starting discovery...'

def rdp = agentForService Services.ROUTE_MAINTENANCE
subscribe topic(rdp);
def datagram = agentForService(org.arl.unet.Services.DATAGRAM)
subscribe topic(datagram);

def n = []
rdp << new org.arl.unet.net.RouteDiscoveryReq(to: phantom, count: attempts)
def ntf

while (ntf = receive(org.arl.unet.net.RouteDiscoveryNtf, timeout)) {
  println("  Discovered neighbor: ${ntf.nextHop}")
  n << ntf.nextHop
}
n = n.unique() // remove duplicates
println("Neighbors are ${n}")