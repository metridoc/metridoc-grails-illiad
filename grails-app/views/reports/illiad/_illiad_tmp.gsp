<strong>
    Borrowing for the Current Fiscal Year
</strong>
<hr/>

<div class='subHeadRow'>Books</div>
<g:render template="/reports/illiad/summary_group"
          model="[summaryData: basicStatsData.books.borrowing,
                  allRowName: allRowName,
                  groups: groups,
                  isBorrowing: true]"
            />
<div class='subHeadRow'>Articles</div>
<g:render template="summary_group"
          model="[summaryData: basicStatsData.articles.borrowing,
                  allRowName: allRowName,
                  groups: groups,
                  isBorrowing: true]"/>
<strong>
    Lending for the Current Fiscal Year
</strong>
<hr/>

<div class='subHeadRow'>Books</div>
<g:render template="summary_group"
          model="[summaryData: basicStatsData.books.lending,
                  allRowName: allRowName,
                  groups: groups,
                  isBorrowing: false]"/>
<div class='subHeadRow'>Articles</div>
<g:render template="summary_group"
          model="[summaryData: basicStatsData.articles.lending,
                  allRowName: allRowName,
                  groups: groups,
                  isBorrowing: false]"/>
</div>

