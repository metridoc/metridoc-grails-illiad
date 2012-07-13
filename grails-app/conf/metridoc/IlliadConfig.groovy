queries {
    illiad {
        transactionCountsBorrowing = '''
		select IFNULL(lg.group_no,-2) as group_no,
		IFNULL(g.group_name,'Other') group_name,
		count(distinct t.transaction_number) transNum,
		IFNULL(sum(billing_amount), 0) as sumFees
		from ill_transaction t
			left join ill_lender_group lg on t.lending_library=lg.lender_code
			left join ill_group g on lg.group_no=g.group_no
			where t.process_type='Borrowing' and t.request_type=? and transaction_date between ? and ?
			{add_condition}
			group by group_no WITH ROLLUP
		'''

        transactionTurnaroundsBorrowing = '''
		select IFNULL(lg.group_no,-2) as group_no,
		AVG(DATEDIFF(receive_date, ship_date)) as turnaroundShpRec,
		AVG(DATEDIFF(ship_date, request_date))as turnaroundReqShp,
		AVG(DATEDIFF(receive_date, request_date)) as turnaroundReqRec
		from ill_transaction t
			left join ill_lender_group lg on t.lending_library=lg.lender_code
			left join ill_tracking bt on t.transaction_number=bt.transaction_number
			where t.process_type='Borrowing' and t.request_type=? and transaction_date between ? and ?
			and request_date is not null and ship_date is not null and receive_date is not null
			and transaction_status='Request Finished'
			group by group_no
		'''

        transactionCountsLending = '''
		select IFNULL(lg.group_no,-2) as group_no,
		IFNULL(g.group_name,'Other') group_name,
		count(distinct t.transaction_number) transNum,
		IFNULL(sum(billing_amount), 0) as sumFees
		from ill_transaction t
			left join ill_lender_group lg on t.lending_library=lg.lender_code
			left join ill_group g on lg.group_no=g.group_no
			where t.process_type='Lending' and t.request_type=? and transaction_date between ? and ?
			{add_condition}
			group by group_no WITH ROLLUP
		'''


        transactionTurnaroundsLending = '''
		select IFNULL(lg.group_no,-2) as group_no,
		AVG(DATEDIFF(lt.completion_date, lt.arrival_date)) as turnaround
		from ill_transaction t
			left join ill_lender_group lg on t.lending_library=lg.lender_code
			left join ill_lending_tracking lt on t.transaction_number=lt.transaction_number
			where t.process_type='Lending' and t.request_type=? and transaction_date between ? and ?
			and lt.completion_date is not null and lt.arrival_date is not null
			and transaction_status='Request Finished'
			group by group_no
		'''

        /* Need to get turnarounds for row Total separately, to avoid double counts
           (because of joining with lending_group)*/
        transactionTotalTurnaroundsBorrowing = '''
		select AVG(DATEDIFF(receive_date, ship_date)) as turnaroundShpRec,
		AVG(DATEDIFF(ship_date, request_date))as turnaroundReqShp,
		AVG(DATEDIFF(receive_date, request_date)) as turnaroundReqRec
		from ill_transaction t
			left join ill_tracking bt on t.transaction_number=bt.transaction_number
			where t.process_type='Borrowing' and t.request_type=? and transaction_date between ? and ?
			and transaction_status='Request Finished' and request_date is not null and ship_date is not null and receive_date is not null
		'''

        transactionTotalTurnaroundsLending = '''
		select AVG(DATEDIFF(lt.completion_date, lt.arrival_date)) as turnaround
		from ill_transaction t
			left join ill_lending_tracking lt on t.transaction_number=lt.transaction_number
			where t.process_type='Lending' and t.request_type=? and transaction_date between ? and ?
			and transaction_status='Request Finished' and lt.completion_date is not null and lt.arrival_date is not null
		'''

        lenderGroupList = '''select * from ill_group'''
    }
}