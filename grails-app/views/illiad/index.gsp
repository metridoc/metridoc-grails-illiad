<%--
  Created by IntelliJ IDEA.
  User: tbarker
  Date: 8/3/12
  Time: 3:26 PM
  To change this template use File | Settings | File Templates.
--%>

<md:report>
    <strong>
        Borrowing for the Current Fiscal Year
    </strong>
    <hr/>

    <div class='subReportBody'>
        <div class='subHeadRow'>Books</div>
        <g:render template="/illiad/summary_group"
                  model="[summaryData: basicStatsData.books.borrowing,
                          allRowName: allRowName,
                          groups: groups,
                          isBorrowing: true]" plugin="metridoc-illiad"/>
        <div class='subHeadRow'>Articles</div>
        <g:render template="/illiad/summary_group"
                  model="[summaryData: basicStatsData.articles.borrowing,
                          allRowName: allRowName,
                          groups: groups,
                          isBorrowing: true]" plugin="metridoc-illiad"/>
    </div>

    <strong>
        Lending for the Current Fiscal Year
    </strong>
    <hr/>

    <div class='subReportBody'>

        <div class='subHeadRow'>Books</div>
        <g:render template="/illiad/summary_group"
                  model="[summaryData: basicStatsData.books.lending,
                          allRowName: allRowName,
                          groups: groups,
                          isBorrowing: false]" plugin="metridoc-illiad"/>
        <div class='subHeadRow'>Articles</div>
        <g:render template="/illiad/summary_group"
                  model="[summaryData: basicStatsData.articles.lending,
                          allRowName: allRowName,
                          groups: groups,
                          isBorrowing: false]" plugin="metridoc-illiad"/>
    </div>

</md:report>