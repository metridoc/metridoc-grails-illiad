<table class="list summary" cellspacing="0">
 <thead>
  <g:if test="${isBorrowing}">
   <tr>
     <th class="mainColHeader" rowspan="2">&nbsp;</th>
     <th rowspan="2"># Requests</th>
     <th rowspan="2"># Filled</th>
     <th rowspan="2">Filled %</th>
     <th colspan="3">Turnaround</th>
     <th rowspan="2"># Exhausted reqs.</th>
     <th rowspan="2">Exhausted %</th>
     <th rowspan="2">Sum fees</th>
   </tr>
   <tr>
     <th>Req-Shp</th>
     <th>Shp-Rec</th>
     <th>Req-Rec</th>
   </tr>
   </thead>
   <tbody>
    <g:set var="currentDataMap" value="${summaryData.get(-1) != null ? summaryData.get(-1):[:]}" />
   <g:render template="summary_row"
	model="[currentDataMap:currentDataMap,
			index:0,
			groupName: allRowName,
			isBorrowing:isBorrowing,
			isTotal:true]" />
			</tbody>
			</table>
			<br>
	<table class="list summary" cellspacing="0">
	<thead>
   <tr>
     <th class="mainColHeader" rowspan="2">&nbsp;</th>
     <th rowspan="2"># Requests</th>
     <th colspan="3">Turnaround</th>
     <th rowspan="2">Sum fees</th>
   </tr>
   <tr>
     <th>Req-Shp</th>
     <th>Shp-Rec</th>
     <th>Req-Rec</th>
   </tr>
  </g:if>
  <g:else>
   <tr>
     <th class="mainColHeader">&nbsp;</th>
     <th># Requests</th>
     <th># Filled</th>
     <th>Filled %</th>
     <th>Turnaround time</th>
     <th># Exhausted reqs.</th>
     <th>Exhausted %</th>
     <th>Sum fees</th>
   </tr>
   </thead>
   <tbody>
    <g:set var="currentDataMap" value="${summaryData.get(-1) != null ? summaryData.get(-1):[:]}" />
   <g:render template="summary_row"
	model="[currentDataMap:currentDataMap,
			index:0,
			groupName: allRowName,
			isBorrowing:isBorrowing,
			isTotal:true]" />
			</tbody>
			</table>
			<br>
	<table class="list summary" cellspacing="0">
	<thead>
   <tr>
     <th class="mainColHeader">&nbsp;</th>
     <th># Requests</th>
     <th># Filled</th>
     <th>Filled %</th>
     <th>Turnaround time</th>
     <th># Exhausted reqs.</th>
     <th>Exhausted %</th>
     <th>Sum fees</th>
   </tr>
   </g:else>
   </thead>
   <tbody>
<g:each var="group" status="i" in="${groups}">
   <g:set var="currentDataMap" value="${summaryData.get(group.group_no.intValue()) != null ? summaryData.get(group.group_no.intValue()): [:]}" />
    <g:render template="summary_row"
	model="[currentDataMap:currentDataMap,
			index:(i+1),
			groupName: group.group_name,
			isBorrowing:isBorrowing]" />
</g:each>
<g:set var="currentDataMap" value="${summaryData.get(-2) != null ? summaryData.get(-2):[:]}" />
   <g:render template="summary_row"
	model="[currentDataMap:currentDataMap,
			index:groups.size()+1,
			groupName: 'Other',
			isBorrowing: isBorrowing]" />
   </tbody></table>