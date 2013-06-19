Ext.define('SF.model.Incident', {
    extend: 'Ext.data.Model',
    fields: ['id',{
        name:
        'incidentType',
        convert: function(value, record){
            return value.name;
        }
    },{
        name:'incidentGroup',
        mapping: function(value, record){
            return value.incidentType.incidentGroup.name;
        }
    },
    {
        name:'incidentName',
        mapping: function(value, record){
            return value.incidentType.name + " - " + value.incidentType.incidentGroup.name;
        }
    },
    {
        name:'date',
        type:'date',
        dateFormat:'c'
    }, 
    'phoneType', 
    'phoneOs', 
    'frequency', 
    'locationTech', 
    'latitude', 
    'longitude',
    'address',
    'position',
    'cellId',
    'subscriber',
    {
        name:'subscriberName',
        mapping: function(value, record){
            return value.subscriber.firstname + " " + value.subscriber.lastname;
        }
    },
    {
        name:'accountName',
        mapping: 'subscriber.account.name'
    },
    'comment',
    'reporter',
    'source',
    {
        name:'source-name',
        convert: function(value, record) {
            if (record.raw.source == 2) {
                return 'Web';
            }
            return 'SayMetrix App';
        }
    },
    {
        name:'source-img',
        convert: function(value, record) {
            if (record.raw.source == 2) {
                return 'web.png';
            }
            return 'mobile.png';
        }
    }]
});


