var util = require("util"),
http = require("http"),  
path = require("path"),  
dgram = require("dgram"),  
url = require("url"),  
filesys = require("fs"),
os = require("os");;

port = 8081;
mobilePort = 8082;
multicastPort = 8099;

hostname = process.argv[2];
multicast = process.argv[3];
fileDirectory = process.argv[4];

util.puts("address for server: " + hostname);
util.puts("multicast address for server: " + multicast);

// Marker function for communications that are significant to the Mac OS client.
// Usually this means they will be parsed.
var protocolOutput = function(s) {
    util.puts(s);
};

var clients = [];
var reportClients = function() {
    protocolOutput("begin current client list:");
    clients.forEach(function (c) {
        protocolOutput(c.name);
    });
    protocolOutput("end current client list.");
};

var addClient = function(client) {
    clients.push(client);
    reportClients();
};

var removeClient = function(client) {
    clients.remove(client);
    reportClients();
};
var files = {};
var fileIndex = 0;

process.stdin.resume();
process.stdin.on('end', function() {
    process.exit(0);
});

Array.prototype.remove = function(e) {
    for (var i = 0; i < this.length; i++) {
        if (e == this[i]) { return this.splice(i, 1); }
    }
};

setInterval(function () {
    clients.forEach(function(c) {
        c.cxn.write('\n');
    });
}, 5000);

var httpServer = http.createServer(function(request, response){  
    var parts = url.parse(request.url, true);
    util.puts("got path: " + parts.pathname + " query: " + util.inspect(parts.query));
    var pathname = parts.pathname;

    if (pathname.match(/^\/sendIntent$/)) {
        util.puts("got intent");
        request.setEncoding('utf-8');
        var allData = "";
        
        request.on('data', function (chunk) {
            allData = allData + chunk;
        });
        request.on('end', function () {
            var intent = JSON.parse(allData);
            util.puts('forwarding intent');

            clients.forEach(function(c) {
                util.puts("    sending intent to client " + c.name + ": " + util.inspect(intent) + "");
                c.cxn.write(allData);
                c.cxn.write("\nend intent\n");
            });
        });
    } else if (pathname.match(/^\/streamIntents/)) {
        var client = {
            name : parts.query.deviceId,
            cxn : response
        };

        // leave the response open for long polling
        addClient(client);

        response.on('timeout', function () {
            util.puts('timeout occurred' + new Error().stack);
        });

        response.on('close', function () {
            removeClient(client);
            util.puts("Removed mobile client. Total: " + clients.length);
            client.cxn.end();
        });
    } else if (pathname.match(/^\/file\//)) {
        var subPath = pathname.replace(/^\/file\//, "");
        var localPath = path.join(fileDirectory, subPath);

        util.puts("sending " + localPath + " to a client");
        response.writeHead(200, {
            'Transfer-Encoding' : 'chunked',
            'Content-Encoding' : 'application/octet-stream'
        });

        var readStream = filesys.createReadStream(localPath);
        util.pump(readStream, response, function (err) {
            if (err) {
                util.puts("error writing " + localPath + " to client: " + err);
            }
        });
    }
});
httpServer.listen(port);  

util.puts("Server Running on " + port);

// setup server discovery listener
var broadcastServer = dgram.createSocket("udp4");
broadcastServer.on("message", function(msg, rinfo) {
    if (msg != "roge") {
        return;
    }

    // send back a response
	util.puts("received query from address " + rinfo.address + ", host name " + os.hostname() + ", sending response");

    var message = new Buffer("SECRETS!" + os.hostname());
    broadcastServer.send(message, 0, message.length, multicastPort, rinfo.address, function (err, bytes) {
        if (err) {
            util.puts("error sending udp message: " + err);
        }
    });
});

broadcastServer.bind(multicastPort);
broadcastServer.setBroadcast(true);
broadcastServer.addMembership(multicast);
