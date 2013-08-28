<div class='subHeadRow'>
    <a href="${createLink(action: "download", params: [borrowing: borrowing, type: type])}">
        <icon class="icon-download-alt" data-toggle="tooltip" data-original-title="Download all available illiad data for this table"></icon></a>
    <span class="aggregation-header" data-toggle="tooltip"
          data-original-title="Items below may not sum to the aggregates since transactions can be in mulltiple groups">${body()}*</span>
</div>