![Mango Logo](https://github.com/infiniteautomation/ma-core-public/blob/main/Core/web/images/logo.png)  

[![DepShield Badge](https://depshield.sonatype.org/badges/infiniteautomation/ma-core-public/depshield.svg)](https://depshield.github.io)  

## Mango Automation Core public code by [Infinite Automation](http://www.infiniteautomation.com)

Mango Automation is a full featured SCADA / HMI solution that offers complete flexibility no matter how large or small your needs are.  Mango can be used in hundreds of configuration, from data protocol gateway to graphic user interface.  With the Mango expansion modules in the Mango Automation store and the ability to build custom modules, you will find everything you need in Mango.  Here is a list of the key features available.

### Speed

Mango is a data logging, control, and monitoring system all in one featuring a browser-based interface. Yet, even on small embedded computers, Mango can host hundreds of data points collected from multiple data sources using multiple protocols. When more powerful equipment is used and MySQL is configured, Mango can easily support thousands or tens of thousands of points on a single instance.

### Multiple Protocols

Mango can receive data from any device for which there is a protocol driver. Currently supported protocols include BACnet I/P & MS/TP, Modbus (ASCII, RTU, TCP, and UDP), OPC DA, 1-wire, SNMP, SQL, HTTP, POP3, NMEA 0183, MBus, DNP3, OpenV, webcams, vmstat, and many proprietary protocols developed by or for hardware vendors. Mango also supports a "virtual" data source that can generate data for benchmarking or testing purposes. More protocols are being added regularly.

### Multiple Datastores

Mango ships with the H2 embedded database so that you don't have to have one of your own. But, Mango can also use MySQL or MSSQL for power users who need performance.

### Custom dashboards

Create dynamic dashboards and pages in Angular JS via a Drag and Drop style design tool built right into the web interface.  In keeping with Mango's Open Source ideology the tool generates HTML and Angular directives that can be edited by hand to allow the user fine grained control of the final dashboard/page.

### Meta points

Use scripts to create new points based upon the values of other points. Based upon Javascript, the most popular scripting language in the world, meta points allow for powerful combinations of point values as well as historical point information.

### Automation Scripting

Now Mango can be a powerful automaion engine. The Mango Scripting component allows you to write complex automation routines based on JavaScript.  These scripts can take advantage of all the data available in your Mango instance and output to any set-able Data Point.

### User-defined events

Tell Mango what events you are interested in. Users can define unlimited event criteria on points to detect conditions such as high and low limits, value changes, state change counts, and run-times.

### Import/Export

Export your configuration to a text file. Save this file for backup and recovery, or use it to import into other instances of Mango to make identical copies. Or, use the file to manage very large configurations easily.

### Event handling

Any events that occur, either system or user defined, can be handled arbitrarily using user-defined event handlers. These handlers can send emails and escalations to mailing lists, or set values in Mango points.

### Security

Your data resides where you install Mango, so you are in control. User permissions are defined by system administrators, and all communications with Mango can be secured with SSL (Secure Socket Layer), ensuring the privacy of your information.

### Data logging

Each point can be configured with its own data logging and log purging characteristics. Logging schedules can be made to be independent of reading schedules.

### Reports

Create and schedule reports for online viewing or email. Download data in CSV format for quick upload into spreadsheets or other data analysis programs.

### Data publishing

Forward information gathered by Mango to other systems in your M2M architecture in on-event fashion for near real-time updating.

### Quick charts and set points

Access and control is quick and up-to-date with Mango's roll-over charts and point set controls. The use of Ajax technology ensures that all of the information displayed is recent and relevent.

### Watch lists

Create your own custom list of points that you want to watch. Easily add and remove points from the list to keep an eye on particular point values and alarms. Create named watch lists with your favorite groups of points. Instantly see multi-point graphs of the points on your list.

### Point hierarchies

Create your own arbitrary hierarchies of points and point folders to organize your information the way you want to see it.

### Graphical views

Use images, graphics, and animations to create dynamic dashboards and graphical representations of your data.  With drag and drop simplisity you can be as creative as you like.

### Point details

View point detail information including current value, detailed tabular and graphical charts, alarms, event detectors, and user permissions. Set point controls are also available.

### Active alarms list

All pages in the application include an indicator of the highest active alarm level. Use it to link to the active alarms list where you can see all active alarms at a glance. Read and add comments and link to point details pages where actions can be taken.

### User memos

Users can comment on events and points so that valuable knowledge is not lost. Event comments are sent with email notifications so that all users are kept up-to-date on system status.

### Event scheduling

Define events based upon time schedules. Events raised by schedules have access to all of the handling functionality that other event types have.

### Audit trail

Changes to all information processing objects cause audit events to be raised, including new objects, changes and deletions. These events pass through the event management system so that all users can independently acknowledge the event.

### Automatic software updates

Be notified when new versions of Mango are available. Simply copy the new Mango version into your installation directory and all of your data will be updated automatically.

### I18N (internationalization)

Mango is fully internationalizable. New languages can be supported simply by translating the label file (and contextual documentation files). Currently translated languages are English, German, Portuguese, Dutch, Chinese and Finnish.

### Remote graphical views

Create portlets on your website that remotely display the views you create in Mango.

### Alarm sounds

Hear sounds when alarms are raised in Mango.

## Contributing to Mango

Mango is open source and always looking for suggestions and additions.  Please review our guidelines for contributions [here](https://github.com/infiniteautomation/.github/blob/master/CONTRIBUTING.md)
