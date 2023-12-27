/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*jshint evil: true, nomen: false, onevar: false, regexp: false, strict: true, boss: true, undef: true, maxlen: 160, curly: true, eqeqeq: true */
/*global document: false, jQuery:false, DOMParser: true, window: false, Wicket: true */

;(function () {
	'use strict';
  
	function initializeWebSocket() {
	  var topics = Wicket.Event.Topic.WebSocket;
  
	  if ('WebSocket' in window) {
		var self = this,
		  url,
		  protocol,
		  WWS = Wicket.WebSocket,
		  port = WWS.port || document.location.port,
		  securePort = WWS.securePort || document.location.port,
		  _port;
  
		protocol = document.location.protocol
		  .replace('https:', 'wss:')
		  .replace('http:', 'ws:');
  
		_port = protocol === 'wss:' ? (securePort ? ':' + securePort : '') : (port ? ':' + port : '');
  
		url = protocol + '//' + document.location.hostname + _port + WWS.contextPath + WWS.filterPrefix + '/wicket/websocket';
  
		url = appendQueryParams(url);
  
		self.ws = new WebSocket(url);
  
		initializeWebSocketEvents.call(this, topics);
	  } else {
		handleWebSocketNotSupported(topics);
	  }
	}
  
	function appendQueryParams(url) {
	  var WWS = Wicket.WebSocket;
  
	  url += ';jsessionid=' + encodeURIComponent(WWS.sessionId);
  
	  if (WWS.pageId !== false) {
		url += '?pageId=' + encodeURIComponent(WWS.pageId);
	  } else if (WWS.resourceName) {
		url += '?resourceName=' + encodeURIComponent(WWS.resourceName);
		if (WWS.connectionToken) {
		  url += '&connectionToken=' + encodeURIComponent(WWS.connectionToken);
		}
	  }
  
	  if (WWS.context) {
		url += '&context=' + encodeURIComponent(WWS.context);
	  }
  
	  url += '&wicket-ajax-baseurl=' + encodeURIComponent(WWS.baseUrl);
	  url += '&wicket-app-name=' + encodeURIComponent(WWS.appName);
  
	  return url;
	}
  
	function initializeWebSocketEvents(topics) {
	  var self = this;
  
	  self.ws.onopen = function (evt) {
		Wicket.Event.publish(topics.Opened, evt);
	  };
  
	  self.ws.onmessage = function (event) {
		handleWebSocketMessage.call(self, event, topics);
	  };
  
	  self.ws.onclose = function (evt) {
		handleWebSocketClose.call(self, evt, topics);
	  };
  
	  self.ws.onerror = function (evt) {
		handleWebSocketError.call(self, evt, topics);
	  };
	}
  
	function handleWebSocketMessage(event, topics) {
	  var message = event.data;
	  if (typeof message === 'string' && message.indexOf('<ajax-response>') > -1) {
		handleAjaxResponse(message);
	  } else {
		Wicket.Event.publish(topics.Message, message);
	  }
	}
  
	function handleAjaxResponse(message) {
	  Wicket.channelManager.schedule(Wicket.WebSocket.MESSAGE_CHANNEL, function () {
		var context = {
		  attrs: {},
		  steps: []
		};
		var xmlDocument = Wicket.Xml.parse(message);
		this.loadedCallback(xmlDocument, context);
		context.steps.push(function () {
		  Wicket.channelManager.done(Wicket.WebSocket.MESSAGE_CHANNEL);
		  return Wicket.ChannelManager.FunctionsExecuter.DONE;
		});
		var executer = new Wicket.ChannelManager.FunctionsExecuter(context.steps);
		executer.start();
	  }.bind(new Wicket.Ajax.Call()));
	}
  
	function handleWebSocketClose(evt, topics) {
	  if (this.ws) {
		this.ws.close();
		this.ws = null;
	  }
	  Wicket.Event.publish(topics.Closed, evt);
	}
  
	function handleWebSocketError(evt, topics) {
	  if (this.ws) {
		this.ws.close();
		this.ws = null;
	  }
	  Wicket.Event.publish(topics.Error, evt);
	}
  
	function handleWebSocketNotSupported(topics) {
	  var errMessage = '[WebSocket.initialize] WebSocket is not supported in your browser!';
	  Wicket.Log.error(errMessage);
	  Wicket.Event.publish(topics.NotSupported, errMessage);
	}
  
	var WicketWebSocket = {
	  ws: null,
  
	  initialize: initializeWebSocket,
  
	  send: function (text) {
		if (this.ws && text) {
		  Wicket.Log.info('[WebSocket.send] Sending: ' + text);
		  this.ws.send(text);
		} else if (!text) {
		  Wicket.Log.error('[WebSocket.send] Cannot send an empty text message!');
		} else {
		  Wicket.Log.error('[WebSocket.send] No open WebSocket connection! Cannot send text message: ' + text);
		}
	  },
  
	  close: function () {
		if (this.ws) {
		  this.ws.close();
		  Wicket.Log.info('[WebSocket.close] Connection closed.');
		} else {
		  Wicket.Log.info('[WebSocket.close] Connection already closed.');
		}
	  }
	};
  
	Wicket.WebSocket = Wicket.Class.create();
	Wicket.WebSocket.MESSAGE_CHANNEL = 'websocketMessage|s';
	Wicket.WebSocket.prototype = WicketWebSocket;
  
	Wicket.WebSocket.createDefaultConnection = function () {
	  if (!Wicket.WebSocket.INSTANCE) {
		Wicket.WebSocket.INSTANCE = new Wicket.WebSocket();
	  }
	};
  
	Wicket.WebSocket.send = function (text) {
	  if (Wicket.WebSocket.INSTANCE) {
		Wicket.WebSocket.INSTANCE.send(text);
	  } else {
		Wicket.Log.error('[WebSocket.send] No default connection available!');
	  }
	};
  
	Wicket.WebSocket.close = function () {
	  if (Wicket.WebSocket.INSTANCE) {
		Wicket.WebSocket.INSTANCE.close();
		delete Wicket.WebSocket.INSTANCE;
	  } else {
		Wicket.Log.info('[WebSocket.close] No default connection to close.');
	  }
	};
  
  })();
