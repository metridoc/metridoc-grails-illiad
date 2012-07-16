<% int filledRequests = currentDataMap.filledRequests != null ? currentDataMap.filledRequests : 0;
int exhaustedRequests = currentDataMap.exhaustedRequests != null ? currentDataMap.exhaustedRequests : 0;
int allRequests = filledRequests + exhaustedRequests %>
<tr class="${(index % 2) == 0 ? 'even' : 'odd'}">
    <td>${groupName}</td>
    <td class="dataCell"><g:formatNumber number="${allRequests}" format="###,###,##0"/></td>
    <g:if test="${!isBorrowing || isTotal}">
        <td class="dataCell"><g:formatNumber number="${filledRequests}" format="###,###,##0"/></td>
        <td class="dataCell"><g:formatNumber number="${allRequests != 0 ? (filledRequests / allRequests) * 100 : 0}"
                                             format="0.00"/></td>
    </g:if>
    <g:if test="${isBorrowing}">
        <td class="dataCell">
            <g:if test="${currentDataMap.turnaroundReqShp != null}">
                <g:formatNumber number="${currentDataMap.turnaroundReqShp}" format="0.00"/>
            </g:if>
            <g:else>--</g:else>
        </td>
        <td class="dataCell">
            <g:if test="${currentDataMap.turnaroundShpRec != null}">
                <g:formatNumber number="${currentDataMap.turnaroundShpRec}" format="0.00"/>
            </g:if>
            <g:else>--</g:else>
        </td>
        <td class="dataCell">
            <g:if test="${currentDataMap.turnaroundReqRec != null}">
                <g:formatNumber number="${currentDataMap.turnaroundReqRec}" format="0.00"/>
            </g:if>
            <g:else>--</g:else>
        </td>
    </g:if>
    <g:else>
        <td class="dataCell">
            <g:if test="${currentDataMap.turnaround != null}">
                <g:formatNumber number="${currentDataMap.turnaround}" format="0.00"/>
            </g:if>
            <g:else>
                --
            </g:else>
        </td>
    </g:else>
    <g:if test="${!isBorrowing || isTotal}">
        <td class="dataCell"><g:formatNumber number="${exhaustedRequests}" format="###,###,##0"/></td>
        <td class="dataCell"><g:formatNumber number="${allRequests != 0 ? (exhaustedRequests / allRequests) * 100 : 0}"
                                             format="0.00"/></td>
    </g:if>
    <td class="dataCell"><g:formatNumber number="${currentDataMap.sumFees != null ? currentDataMap.sumFees : 0}"
                                         format="###,###,##0"/></td>
</tr>