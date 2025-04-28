function filterBloodGroup() {
    var filterValue = document.getElementById("bloodGroupFilter").value;
    var rows = document.querySelectorAll("#donationTable .person-row");

    rows.forEach(function(row) {
        var bloodGroup = row.getAttribute("data-blood-group");

        if (filterValue === "" || bloodGroup === filterValue) {
            row.style.display = "";
        } else {
            row.style.display = "none";
        }
    });
}

function filterLocation() {
    var filterValue = document.getElementById("locationFilter").value;
    var rows = document.querySelectorAll("#donationTable .person-row");

    rows.forEach(function(row) {
        var location = row.getAttribute("data-location");

        if (filterValue === "" || location === filterValue) {
            row.style.display = "";
        } else {
            row.style.display = "none";
        }
    });
}

function sortTable() {
    var table = document.getElementById("donationTable");
    var rows = Array.from(table.rows).slice(1);
    var sortOption = document.getElementById("sortOption").value;

    rows.sort(function(rowA, rowB) {
        let cellA, cellB;
        
        if (sortOption === "bloodGroup") {
            cellA = rowA.cells[1].innerText;
            cellB = rowB.cells[1].innerText;
        } else if (sortOption === "donation-date") {
            // Sort by donation date (column 2)
            cellA = rowA.cells[2].innerText;
            cellB = rowB.cells[2].innerText;
            
            // Handle N/A values
            if (cellA === 'N/A') cellA = '1900-01-01';
            if (cellB === 'N/A') cellB = '1900-01-01';
            
            // Return most recent dates first
            return new Date(cellB) - new Date(cellA);
        } else {
            // Location (city/state)
            cellA = rowA.cells[3].innerText + ', ' + rowA.cells[4].innerText;
            cellB = rowB.cells[3].innerText + ', ' + rowB.cells[4].innerText;
        }

        return cellA.localeCompare(cellB);
    });

    rows.forEach(function(row) {
        table.appendChild(row);
    });
}

