<%@ page import="metridoc.utils.DateUtil" %>
<%--
  Created by IntelliJ IDEA.
  User: tbarker
  Date: 8/3/12
  Time: 3:26 PM
  To change this template use File | Settings | File Templates.
--%>

<md:report>

    <div id="updateInfo">last update: ${lastUpdated}</div>
    <md:header>Borrowing for the Current Fiscal Year (${month} to Present)</md:header>

    <div class='subReportBody'>
        <tmpl:aggregationHeader type="Loan" borrowing="true">Books</tmpl:aggregationHeader>
        <g:render template="/illiad/summary_group"
                  model="[summaryData: basicStatsData.books.borrowing,
                          allRowName: allRowName,
                          groups: groups,
                          isBorrowing: true]"/>
        <tmpl:aggregationHeader type="Article" borrowing="true">Articles</tmpl:aggregationHeader>
        <g:render template="/illiad/summary_group"
                  model="[summaryData: basicStatsData.articles.borrowing,
                          allRowName: allRowName,
                          groups: groups,
                          isBorrowing: true]"/>
    </div>

    <md:header>Lending for the Current Fiscal Year (${month} to Present)</md:header>

    <div class='subReportBody'>

        <tmpl:aggregationHeader type="Loan" borrowing="false">Books</tmpl:aggregationHeader>
        <g:render template="/illiad/summary_group"
                  model="[summaryData: basicStatsData.books.lending,
                          allRowName: allRowName,
                          groups: groups,
                          isBorrowing: false]"/>
        <tmpl:aggregationHeader type="Article" borrowing="false">Articles</tmpl:aggregationHeader>
        <g:render template="/illiad/summary_group"
                  model="[summaryData: basicStatsData.articles.lending,
                          allRowName: allRowName,
                          groups: groups,
                          isBorrowing: false]"/>
    </div>

</md:report>