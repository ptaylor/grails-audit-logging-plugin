package grails.plugins.orm.auditable

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
*/
import grails.plugins.*
import org.grails.datastore.mapping.core.*

/**
 * @author Robert Oschwald
 * @author Aaron Long
 * @author Shawn Hartsock
 * @author Graeme Rocher
 *
 * Credit is due to the following other projects,
 * first is Kevin Burke's HibernateEventsGrailsPlugin
 * second is the AuditLogging post by Rob Monie at
 * http://www.hibernate.org/318.html
 *
 * Combined the two sources to create a Grails
 * Audit Logging plugin that will track individual
 * changes to columns.
 *
 * See Documentation:
 * http://grails.org/plugin/audit-logging
 *
 */

class AuditLoggingGrailsPlugin extends Plugin {

    def grailsVersion = '3.0.0 > *'

    def title = "Audit Logging Plugin"
    def authorEmail = "roos@symentis.com"
    def description = """ Automatically log change events for domain objects.
The Audit Logging plugin additionally adds an instance hook to domain objects that allows you to hang Audit events off of them.
The events include onSave, onChange, and onDelete.
When called, the event handlers have access to oldObj and newObj definitions that will allow you to take action on what has changed.
    """

    String documentation = 'http://grails.org/plugin/audit-logging'
    String license = 'APACHE'
    String organization = [name: "symentis GmbH", url: "http://www.symentis.com/"]
    def developers = [
        [ name: 'Robert Oschwald', email: 'roos@symentis.com' ],
        [ name: 'Elmar Kretzer', email: 'elkr@symentis.com' ],
        [ name: 'Aaron Long', email: 'longwa@gmail.com' ]
    ]
    def issueManagement = [system: 'GitHub', url: 'https://github.com/robertoschwald/grails-audit-logging-plugin/issues']
    def scm = [url: 'https://github.com/robertoschwald/grails-audit-logging-plugin']

    def dependsOn = [:]
    def loadAfter = ['core', 'dataSource']

    // Register generic GORM listener
    void doWithApplicationContext() {
        def application = grailsApplication
        def config = AuditLoggingConfigUtils.auditConfig
        boolean disabled = config.getProperty("disabled", Boolean, false)
        boolean stampEnabled = config.getProperty("stampEnabled", Boolean, true)
        boolean stampAlways = config.getProperty("stampAlways", Boolean, false)

        String stampCreatedBy = config.getProperty("stampCreatedBy", String, "createdBy")
        String stampLastUpdatedBy = config.getProperty("stampLastUpdatedBy", String, "lastUpdatedBy")
        boolean verbose = config.getProperty("verbose", Boolean, false)
        boolean nonVerboseDelete = config.getProperty("nonVerboseDelete", Boolean, false)
        boolean logFullClassName = config.getProperty("logFullClassName", Boolean, false)
        boolean transactional = config.getProperty("transactional", Boolean, false)
        boolean logIds = config.getProperty("logIds", Boolean, false)
        String sessionAttribute = config.getProperty("sessionAttribute", String, "")
        String actorKey = config.getProperty("actorKey", String, "")
        Closure actorClosure = config.getProperty("actorClosure", Closure, AuditLogListenerUtil.actorDefaultGetter)
        String propertyMask = config.getProperty("propertyMask", String, "**********")


        applicationContext.getBeansOfType(Datastore).each { String key, Datastore datastore ->
            // Note: Some datastores do not hold config property (e.g. mongodb)
            boolean dataStoreDisabled = datastore.hasProperty("config") ? datastore.config.auditLog.disabled : false
            // Don't register the listener if we are disabled
            if (!disabled && !dataStoreDisabled) {
                def listener = new AuditLogListener(datastore)
                listener.grailsApplication = application
                listener.stampEnabled = stampEnabled
                listener.stampAlways = stampAlways
                listener.stampCreatedBy = stampCreatedBy
                listener.stampLastUpdatedBy = stampLastUpdatedBy
                listener.verbose = verbose
                listener.nonVerboseDelete = nonVerboseDelete
                listener.logFullClassName = logFullClassName
                listener.transactional = transactional
                listener.sessionAttribute = sessionAttribute
                listener.actorKey = actorKey
                listener.actorClosure = actorClosure
                listener.defaultIgnoreList = AuditLoggingConfigUtils.auditConfig.defaultIgnore?.asImmutable() ?: ['version', 'lastUpdated'].asImmutable()
                listener.defaultMaskList = AuditLoggingConfigUtils.auditConfig.defaultMask?.asImmutable() ?: ['password'].asImmutable()
                listener.propertyMask = propertyMask
                listener.replacementPatterns = AuditLoggingConfigUtils.auditConfig.replacementPatterns
                listener.logIds = logIds
                applicationContext.addApplicationListener(listener)
            }
        }
    }
}
