package metridoc.illiad

import groovy.sql.Sql
import java.sql.SQLException

class IlliadWorkflowService {

    def dataSource_from_illiad
    def sql

    static final LENDER_ADDRESSES_ALL = "LenderAddressesAll"
    static final LENDER_ADDRESSES = "LenderAddresses"
    static final USERS = "Users"
    static final USERS_ALL = "UsersAll"

    def _lenderTableName
    def _userTableName

    def getSql() {
        if(sql) return sql

        sql = new Sql(dataSource_from_illiad)
    }

    def getLenderTableName() {
        if(_lenderTableName) return _lenderTableName

        _lenderTableName = pickTable(LENDER_ADDRESSES_ALL, LENDER_ADDRESSES)
    }

    def getUserTableName() {
        if(_userTableName) return _userTableName

        _userTableName = pickTable(USERS, USERS_ALL)
    }

    private pickTable(option1, option2) {
        if(tableExists(option1)) {
            return option1
        } else {
            return option2
        }
    }

    private tableExists(tableName) {
        try {
            getSql().execute("select count(*) from $tableName" as String)
            return true
        } catch (SQLException e) {
            //table does not exist
            return false
        }
    }
}
