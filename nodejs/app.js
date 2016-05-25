//GPIO init
var gpio = require('onoff').Gpio;
var relay1 = new gpio(2, 'high');
var relay2 = new gpio(3, 'high');
var relay3 = new gpio(17, 'high');
var relay4 = new gpio(27, 'high');

//NETPIE init
var MicroGear = require('microgear');
const KEY    = '';
const SECRET = '';
const APPID     = '';

//NETPIE ACTIONS
var microgear = MicroGear.create({
    key : KEY,
    secret : SECRET
});

microgear.on('connected', function() {
    console.log('Connected to NETPIE already');
    microgear.setAlias("relay");
    microgear.subscribe("/relay");
});

microgear.on('message', function(topic,body) {
	var relay_number = body;
    if(relay_number == '1') {
           var state = relay1.readSync();
           relay1.writeSync(state ^ 1);
           console.log("Relay 1 is " + (state ? "on" : "off"));
    }
    if(relay_number == '2') {
           var state = relay2.readSync();
           relay2.writeSync(state ^ 1);
           console.log("Relay 2 is " + (state ? "on" : "off"));
    }
    if(relay_number == '3') {
           var state = relay3.readSync();
           relay3.writeSync(state ^ 1); 
           console.log("Relay 3 is " + (state ? "on" : "off"));
    }
    if(relay_number == '4') {
           var state = relay4.readSync();
           relay4.writeSync(state ^ 1);
           console.log("Relay 4 is " + (state ? "on" : "off"));
    }
    console.log('incoming : '+ topic +' : '+body);
});

microgear.on('closed', function() {
    console.log('Closed...');
});

microgear.connect(APPID);

