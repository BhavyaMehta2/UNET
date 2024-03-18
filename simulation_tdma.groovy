import org.arl.fjage.WakerBehavior
import org.arl.fjage.*
import org.arl.unet.*
import org.arl.unet.phy.*
import org.arl.unet.sim.*
import org.arl.unet.sim.channels.*
import static org.arl.unet.Services.*
import static org.arl.unet.phy.Physical.*

int countNodes = 4
int depthBase = 1000
int depthData = 1100
int radius = 1000
def nodes = 1..countNodes                      // list with n nodes
def T = 2.hours                       // simulation duration
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

println '''
Simulation
=====================

TX Count\tRX Count\tOffered Load\tThroughput
--------\t--------\t------------\t----------'''

simulate T, {  
    nodes.each { myAddr ->
      def myNode = node "${myAddr}", address: myAddr, location: nodeLocation[myAddr]

      myNode.startup = {
        def phy = agentForService PHYSICAL
        if(myAddr!=1)
        {
          add new WakerBehavior(1500*(myAddr-1), {
            add new TickerBehavior(1500*3, {
              phy << new ClearReq()
              phy << new TxFrameReq(to: 1, type: Physical.DATA)
            })
          })
        }
      }
    }
  }

  println sprintf('%6d\t\t%6d\t\t%7.3f\t\t%7.3f',
    [trace.txCount, trace.rxCount, trace.offeredLoad, trace.throughput])