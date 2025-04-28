function initAutocomplete() {
    const addressInput = document.getElementById('address');
    if (!addressInput) {
        console.warn('Address input field not found');
        return;
    }
    
    const autocomplete = new google.maps.places.Autocomplete(addressInput);
    autocomplete.setFields(['address_components', 'geometry']);

    autocomplete.addListener('place_changed', () => {
        const place = autocomplete.getPlace();
        console.log('Place selected:', place);

        // Get latitude/longitude
        if (place.geometry) {
            const latField = document.getElementById('latitude');
            const lngField = document.getElementById('longitude');
            
            if (latField) latField.value = place.geometry.location.lat();
            if (lngField) lngField.value = place.geometry.location.lng();
            console.log('Set coordinates:', place.geometry.location.lat(), place.geometry.location.lng());
        }

        // Reset fields
        const cityField = document.getElementById('city');
        const stateField = document.getElementById('state');
        const zipField = document.getElementById('zipCode'); // This matches your Thymeleaf template

        if (cityField) cityField.value = '';
        if (stateField) stateField.value = '';
        if (zipField) zipField.value = '';

        if (place.address_components) {
            place.address_components.forEach(component => {
                const types = component.types;
                console.log('Component:', component.long_name, types);

                if (types.includes('locality')) {
                    // City
                    if (cityField) cityField.value = component.long_name;
                } else if (types.includes('administrative_area_level_1')) {
                    // State
                    if (stateField) stateField.value = component.long_name;
                } else if (types.includes('postal_code')) {
                    // Zip Code - match the correct field ID
                    if (zipField) zipField.value = component.long_name;
                }
            });
        }
    });
}

// Wait for DOM to be fully loaded
document.addEventListener('DOMContentLoaded', function() {
    // Initialize when the address field becomes editable
    const editButton = document.querySelector('.edit-btn');
    if (editButton) {
        editButton.addEventListener('click', initAutocomplete);
    } else {
        // If no edit button, initialize on page load
        initAutocomplete();
    }
});