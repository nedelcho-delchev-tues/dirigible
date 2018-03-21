exports.getTemplate = function() {
	return {
		"name": "Message Listener",
		"description": "Listener for a message with a simple Javascript handler",
		"sources": [
		{
			"location": "/template-listener/listener.template", 
			"action": "generate",
			"rename": "{{fileName}}.listener"
		},
		{
			"location": "/template-listener/handler.js.template", 
			"action": "generate",
			"rename": "{{fileName}}-handler.js"
		},
		{
			"location": "/template-listener/trigger.js.template", 
			"action": "generate",
			"rename": "{{fileName}}-trigger.js"
		}],
		"parameters": []
	};
};
