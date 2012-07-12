/*
 * Copyright 2010 Trustees of the University of Pennsylvania Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package metridoc.workflows

import groovy.sql.Sql
import metridoc.dsl.JobBuilder

/**
 * script used to control Illiad to Metridoc ingestion and update process
 *
 * User: pkeavney
 * Date: 3/15/12
 */

includeTargets << new File("scripts/_IlliadConfig.groovy")

JobBuilder.isJob(this)

target(runIlliadWorkflow: "runs the entire workflow for illiad") {
    grailsConsole.info "running illiad"
    depends(configureDataSources)

    depends(
        createSqlClasses,
        updateIlliadSchemas,
        clearingIlliadTables,
        migrateData,
        doUpdateBorrowing,
        doUpdateLending,
        doUpdateDemographics
    )
}

target(createSqlClasses: "creates instances of groovy.sql.Sql classes that will perform raw sql against illiad and the repository") {
    illiadFromSql = new Sql(dataSource_from_illiad)
    illiadDestinationSql = new Sql(dataSource_illiad)
}

target(updateIlliadSchemas: "updates the database in the destination dataSource") {
    depends(configureDataSources)
    profile("checking and updating illiad schemas") {
        runLiquibase(schema: "schemas/illiad/illiadSchema.xml", dataSource: dataSource_illiad)
    }
}

target(clearingIlliadTables: "truncates all tables") {
    depends(createSqlClasses)
    profile("truncating illiad tables") {
        truncateIlliadTablesInRepository(illiadDestinationSql)
    }
}

target(migrateData: "migrates data from illiad to local mysql instance") {

    grailsConsole.info "beginning migration from illiad to repository"

    profile("illiad full migration") {
        [
            ill_group: groupSqlStmt,
            ill_lender_group: groupLinkSqlStmt,
            ill_lender_info: lenderAddrSqlStmt,
            ill_reference_number: referenceNumberSqlStmt,
            ill_transaction: transactionSqlStmt,
            ill_lending: lendingSqlStmt,
            ill_borrowing: borrowingSqlStmt,
            ill_user_info: userSqlStmt

        ].each {key, value ->
            ant.echo(message: "migrating to ${key} using \n    ${value}")
            profile("migration ${key}") {
                runRoute {
                    from("sqlplus:${value}?dataSource=dataSource_from_illiad").to("sqlplus:${key}?dataSource=dataSource_illiad")
                }
            }
        }
    }
}

target(doUpdateLending: "updates lending tables in the destination data source") {
    updateLending(illiadDestinationSql)
}

target(doUpdateBorrowing: "updates the borrowing tables") {
    updateBorrowing(illiadDestinationSql)
}

target(doUpdateDemographics: "updating demographic information") {
    updateDemographics(illiadDestinationSql)
}


