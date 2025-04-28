document.addEventListener('DOMContentLoaded', () => {
    const editButton = document.querySelector('.edit-btn');
    const saveButton = document.querySelector('.save-btn');
    const inputs = document.querySelectorAll('#phone, #address, #city, #state, #zipCode');
    const bloodGroupSelect = document.getElementById('bloodGroup');

    editButton.addEventListener('click', () => {
        inputs.forEach(input => {
            input.removeAttribute('readonly');
            input.style.background = "#fff";
            input.style.border = "1px solid #951a1a";
        });
        
        // Enable blood group selection
        bloodGroupSelect.disabled = false;
        bloodGroupSelect.style.background = "#fff";
        bloodGroupSelect.style.border = "1px solid #951a1a";
        
        editButton.style.display = 'none';
        saveButton.style.display = 'block';
    });
});