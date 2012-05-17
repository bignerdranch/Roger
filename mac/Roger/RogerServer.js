var sys = require("util"),
net = require("net"),  
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
sys.puts("address for server: " + hostname);
sys.puts("multicast address for server: " + multicast);
var clients = [];
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

var httpServer = http.createServer(function(request, response){  
    var parts = url.parse(request.url, true);
    sys.puts("got path: " + parts.pathname + " query: " + parts.query);

    if (parts.pathname == "/sendIntent") {
        request.setEncoding('utf-8');
        var allData = "";
        
        request.on('data', function (chunk) {
            allData = allData + chunk;
        });
        request.on('end', function () {
            var intent = JSON.parse(allData);
            sys.puts('intent received: ' + sys.inspect(intent));
            sys.puts('    txnId: ' + intent["extras"]["com.bignerdranch.franklin.roger.EXTRA_LAYOUT_TXN_ID"]);
        });

    } else if (parts.pathname == "/post") {
        // Desktop client is posting a file
        var apk = parts.query['apk'];
        var layout = parts.query['layout'];
		var type = parts.query['type'];
		var pack = parts.query['pack'];
		var minSdk = parts.query['minSdk'];
		var txnId = parts.query['txnId'];
        sys.puts("parameters: " + 
            apk + ' - apk ' + 
            layout + ' - layout ' + 
            type + ' - type ' + 
            pack + ' - pack ' + 
            minSdk + ' - minSdk ' + 
            txnId + ' - txnId ');
        sys.puts("posting apk: " + apk + " layout: " + layout);
        response.writeHeader(200);
		response.end();

		fileIndex++;
		files[fileIndex] = apk;
        clients.forEach(function(s) {
			sys.puts("sending data to a client");
			s.write(layout + "\n" + type + "\n" + fileIndex + "\n" + pack + "\n" + minSdk + "\n" + txnId + "--", "utf8");
			s.end();
        });  
    } else if (parts.pathname == "/get") {
        var hash = parts.query['hash'];
        var fileName = files[hash];

        sys.puts("sending " + fileName + " to a client");
        response.writeHead(200, {
            'Transfer-Encoding' : 'chunked',
            'Content-Encoding' : 'application/octet-stream'
        });

        var readStream = filesys.createReadStream(fileName);
        sys.pump(readStream, response, function (err) {
            if (err) {
                sys.puts("error writing " + fileName + " to client: " + err);
            }
        });
    }
});
httpServer.listen(port);  

sys.puts("Server Running on " + port);

var fileServer = net.createServer(function (stream) {
    clients.push(stream);

    stream.setTimeout(0);
    stream.setEncoding("utf8");

    stream.addListener("connect", function () {
        sys.puts("Added mobile client. Total: " + clients.length);
        stream.pipe(stream);
    });

    stream.addListener("end", function() {
        clients.remove(stream);
        sys.puts("Removed mobile client. Total: " + clients.length);
        stream.end();
    });
});
fileServer.listen(mobilePort, hostname);
sys.puts("Mobile server Running on " + hostname + ":" + mobilePort);

// setup server discovery listener
var broadcastServer = dgram.createSocket("udp4");
broadcastServer.on("message", function(msg, rinfo) {
    // send back a response
	sys.puts("received query from address " + rinfo.address + ", host name " + os.hostname() + ", sending response");

    var message = new Buffer("SECRETS!" + os.hostname());
    broadcastServer.send(message, 0, message.length, multicastPort, rinfo.address, function (err, bytes) {
        if (err) {
            sys.puts("error sending udp message: " + err);
        }
    });
});

broadcastServer.bind(multicastPort);
broadcastServer.setBroadcast(true);
broadcastServer.addMembership(multicast);
