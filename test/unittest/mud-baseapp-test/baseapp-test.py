from mininet.node import OVSController
from mininet.cli import CLI
from mininet.log import setLogLevel
from mininet.net import Mininet
from mininet.node import RemoteController
from mininet.topo import Topo
import pdb
import subprocess
import argparse
import os
import sys
import signal
from distutils.spawn import find_executable
from subprocess import call
import time
import requests
import json
from mininet.log import setLogLevel 
import unittest
import re
import os

class TestAccess(unittest.TestCase) :
   def setUp(self):
      pass

   def testPing(self):
      result = h1.cmdPrint("ping 10.0.0.2 -c 10 -q")
      self.assertTrue(re.search("0% packet loss",result) is not None, "Expecting a successful ping")
      result = h2.cmdPrint("ping 10.0.0.3 -c 10 -q")
      self.assertTrue(re.search("0% packet loss",result) is not None, "Expecting a successful ping")
      result = h3.cmdPrint("ping 10.0.0.4 -c 10 -q")
      self.assertTrue(re.search("0% packet loss",result) is not None, "Expecting a successful ping")
      result = h4.cmdPrint("ping 10.0.0.5 -c 10 -q")
      self.assertTrue(re.search("0% packet loss",result) is not None, "Expecting a successful ping")
      result = h5.cmdPrint("ping 10.0.0.6 -c 10 -q")
      self.assertTrue(re.search("0% packet loss",result) is not None, "Expecting a successful ping")
      result = h6.cmdPrint("ping 10.0.0.7 -c 10 -q")
      self.assertTrue(re.search("0% packet loss",result) is not None, "Expecting a successful ping")
      result = h6.cmdPrint("ping 10.0.0.7 -c 10 -q")
      self.assertTrue(re.search("0% packet loss",result) is not None, "Expecting a successful ping")
      result = h7.cmdPrint("ping 10.0.0.8 -c 10 -q")
      self.assertTrue(re.search("0% packet loss",result) is not None, "Expecting a successful ping")

   def tearDown(self):
      pass

def cli():
    global net,c1,s1,s2,s3
    global hosts
    cli = CLI( net )
    for h in hosts:
        h.terminate()
    net.stop()

def setupTopology(controller_addr):
    global net,c1,s1,s2,s3
    global h1,h2,h3,h4,h5,h6,h7,h8,h9,h10
    global hosts

    hosts = []
    "Create and run multiple link network"

    net = Mininet(controller=RemoteController)

    print "mininet created"

    c1 = net.addController('c1', ip=controller_addr,port=6653)


    # h1: IOT Device.
    # h2 : StatciDHCPD
    # h3 : router / NAT
    # h4 : Non IOT device.

    h1 = net.addHost('h1')
    h2 = net.addHost('h2')
    h3 = net.addHost('h3')
    h4 = net.addHost('h4')
    h5 = net.addHost('h5')
    h6 = net.addHost('h6')
    h7 = net.addHost('h7')
    h8 = net.addHost('h8')
    h9 = net.addHost('h9')
    h10 = net.addHost('h10')

    hosts.append(h1)
    hosts.append(h2)
    hosts.append(h3)
    hosts.append(h4)
    hosts.append(h5)
    hosts.append(h6)
    hosts.append(h7)
    hosts.append(h8)
    hosts.append(h9)
    hosts.append(h10)

    s2 = net.addSwitch('s2',dpid="2")
    s3 = net.addSwitch('s3',dpid="3")
    s1 = net.addSwitch('s1',dpid="1")

    s1.linkTo(h1)
    s1.linkTo(h2)
    s1.linkTo(h3)
    s1.linkTo(h4)
    s1.linkTo(h5)
    s1.linkTo(h6)
    s1.linkTo(h7)

    s2.linkTo(h8)
    s3.linkTo(h8)

    s3.linkTo(h9)
    s3.linkTo(h10)

    # S2 is the NPE switch.
    # Direct link between S1 and S2
    s1.linkTo(s2)


    h8.cmdPrint('echo 0 > /proc/sys/net/ipv4/ip_forward')
    # Flush old rules.
    h8.cmdPrint('iptables -F')
    h8.cmdPrint('iptables -t nat -F')
    h8.cmdPrint('iptables -t mangle -F')
    h8.cmdPrint('iptables -X')
    h8.cmdPrint('echo 1 > /proc/sys/net/ipv4/ip_forward')

    # Set up h3 to be our router (it has two interfaces).
    # Set up iptables to forward as NAT
    h8.cmdPrint('iptables -t nat -A POSTROUTING -o h8-eth1 -s 10.0.0.0/24 -j MASQUERADE')

    net.build()
    net.build()
    c1.start()
    s1.start([c1])
    s2.start([c1])
    s3.start([c1])

    net.start()
     

    # Clean up any traces of the previous invocation (for safety)


    h1.setMAC("00:00:00:00:00:11","h1-eth0")
    h2.setMAC("00:00:00:00:00:22","h2-eth0")
    h3.setMAC("00:00:00:00:00:33","h3-eth0")
    h4.setMAC("00:00:00:00:00:44","h4-eth0")
    h5.setMAC("00:00:00:00:00:55","h5-eth0")
    h6.setMAC("00:00:00:00:00:66","h6-eth0")
    h7.setMAC("00:00:00:00:00:77","h7-eth0")
    h8.setMAC("00:00:00:00:00:88","h8-eth0")
    h9.setMAC("00:00:00:00:00:99","h9-eth0")
    h10.setMAC("00:00:00:00:00:100","h10-eth0")

    
    # Set up a routing rule on h2 to route packets via h3
    h1.cmdPrint('ip route del default')
    h1.cmdPrint('ip route add default via 10.0.0.8 dev h1-eth0')

    # Set up a routing rule on h2 to route packets via h3
    h2.cmdPrint('ip route del default')
    h2.cmdPrint('ip route add default via 10.0.0.8 dev h2-eth0')

    # Set up a routing rule on h2 to route packets via h7
    h3.cmdPrint('ip route del default')
    h3.cmdPrint('ip route add default via 10.0.0.8 dev h3-eth0')

    # Set up a routing rule on h2 to route packets via h3
    h4.cmdPrint('ip route del default')
    h4.cmdPrint('ip route add default via 10.0.0.8 dev h4-eth0')

    # Set up a routing rule on h5 to route packets via h3
    h5.cmdPrint('ip route del default')
    h5.cmdPrint('ip route add default via 10.0.0.8 dev h5-eth0')

    # h6 is a localhost.
    h6.cmdPrint('ip route del default')
    h6.cmdPrint('ip route add default via 10.0.0.8 dev h6-eth0')

    # The IDS runs on h8
    h7.cmdPrint('ip route del default')
    h7.cmdPrint('ip route add default via 10.0.0.8 dev h7-eth0')

    # h9 is our fake host. It runs our "internet" web server.
    h9.cmdPrint('ifconfig h9-eth0 203.0.113.13 netmask 255.255.255.0')
    # Start a web server there.
    h9.cmdPrint('python http-server.py -H 203.0.113.13&')

    # h10 is our second fake host. It runs another internet web server that we cannot reach
    h10.cmdPrint('ifconfig h10-eth0 203.0.113.14 netmask 255.255.255.0')
    # Start a web server there.
    h10.cmdPrint('python http-server.py -H 203.0.113.14&')


    # Start dnsmasq (our dns server).
    h5.cmdPrint('/usr/sbin/dnsmasq --server  10.0.4.3 --pid-file=/tmp/dnsmasq.pid'  )

    # Set up our router routes.
    h8.cmdPrint('ip route add 203.0.113.13/32 dev h8-eth1')
    h8.cmdPrint('ip route add 203.0.113.14/32 dev h8-eth1')
    h8.cmdPrint('ifconfig h8-eth1 203.0.113.1 netmask 255.255.255.0')
    

    print "*********** System ready *********"

if __name__ == '__main__':
    setLogLevel( 'info' )
    parser = argparse.ArgumentParser()
    # defaults to the address assigned to my VM
    parser.add_argument("-c",help="Controller host address",default=os.environ.get("CONTROLLER_ADDR"))

    parser.set_defaults(test=False)

    args = parser.parse_args()
    controller_addr = args.c

    if controller_addr is None:
       print "Please specify controller addr using -c or environment variable."
       sys.exit()

    test = args.test


    cmd = ['sudo','mn','-c']
    proc = subprocess.Popen(cmd,shell=False, stdin= subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    proc.wait()
    
    # Pkill dnsmasq. We will start one up later on h3
    if os.path.exists("/tmp/dnsmasq.pid"):
    	f = open('/tmp/dnsmasq.pid')
    	pid = int(f.readline())
    	try:
    	   os.kill(pid,signal.SIGTERM)
	except:
	   print "Failed to kill dnsmasq check if process is running"


    print("IMPORTANT : append 10.0.0.5 to resolv.conf")


    setupTopology(controller_addr)

    if os.environ.get("UNITTEST") is not None and os.environ.get("UNITTEST") == '1' :
	time.sleep(10)
        unittest.main()
    else:
        cli()

