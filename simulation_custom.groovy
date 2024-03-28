import org.arl.unet.addr.AddressResolution
import org.arl.fjage.*
import org.arl.unet.*
import org.arl.unet.phy.*
import org.arl.unet.sim.*
import org.arl.unet.sim.channels.*

for(int i = 1; i<=10; i++)
{
int countNodes = 4
int depthBase = 1000
int depthData = 1100
int radius = 1000
def nodes = 1..countNodes                      // list with n nodes
def T = 1.hours                       // simulation duration
trace.warmup = 15.minutes             // collect statistics after a while

def loc = new LocationGen()
def nodeLocation = loc.generate(countNodes, radius, depthBase, depthData);

///////////////////////////////////////////////////////////////////////////////

channel.model = ProtocolChannelModel

// modem.dataRate = [2400, 2400].bps
// modem.frameLength = [2400/8, 2400/8].bytes
modem.headerLength = 0
modem.preambleDuration = 0
modem.txDelay = 0

///////////////////////////////////////////////////////////////////////////////
// simulation details

// println '''
// Simulation
// =====================

// TX Count\tRX Count\tOffered Load\tThroughput
// --------\t--------\t------------\t----------'''

setup1 = { c -> // Define setup1 closure for node configuration
  c.add 'echo', new BaseNodeCompat(countNodes)
  c.add 'arp', new AddressResolution()
}

setup2 = { c -> // Define setup2 closure for node configuration
  c.add 'echo', new DataNodeCompat(countNodes)
  c.add 'arp', new AddressResolution()
}

simulate T, {
  nodes.each{n ->
    if(n==1)
      node "N", address:n, location: nodeLocation[n], web:8080+n, stack:setup1
    else
      node "N"+(n-1), address:n, location: nodeLocation[n], web:8080+n, stack:setup2
  }
}

println sprintf('%6d\t\t%6d\t\t%7.3f\t\t%7.3f',
    [trace.txCount, trace.rxCount, trace.offeredLoad, trace.throughput])
}