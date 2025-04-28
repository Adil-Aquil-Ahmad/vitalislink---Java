// Dropdown functions
function myFunction(event) {
  // Prevent the event from bubbling up
  event.stopPropagation();

  const dropdownContent = document.getElementById("myDropdown");
  const isVisible = dropdownContent.classList.contains("show");
  
  // Close all dropdowns first
  closeAllDropdowns();

  // Toggle this dropdown
  if (!isVisible) {
      dropdownContent.classList.add("show");
  }
}

function closeAllDropdowns() {
  const dropdowns = document.getElementsByClassName("dropdown-content");
  for (let i = 0; i < dropdowns.length; i++) {
      dropdowns[i].classList.remove("show");
  }
}

// Flash message timeout
setTimeout(() => {
    const flashMessages = document.querySelectorAll('.flash-messages');
    flashMessages.forEach(msg => msg.style.display = 'none');
}, 5000);

// Toggle hamburger menu
function toggleMenu() {
    const hamburger = document.querySelector('.hamburger');
    const navMenu = document.getElementById('navMenu');
    
    if (!navMenu || !hamburger) return;
    
    // Toggle active class on hamburger button
    hamburger.classList.toggle('active');
    
    // Toggle menu visibility
    navMenu.classList.toggle('active');
    
    // Set aria-expanded for accessibility
    const isExpanded = navMenu.classList.contains('active');
    hamburger.setAttribute('aria-expanded', isExpanded);
    
    // Disable scroll when menu is open
    document.body.style.overflow = isExpanded ? 'hidden' : '';
    
    // Close dropdown when closing menu
    if (!isExpanded) {
        closeDropdown();
    }
    
    console.log('Menu toggled, active:', isExpanded);
}

// Toggle user dropdown
function toggleDropdown(event) {
    if (event) {
        event.stopPropagation();
    }
    
    const dropdownContent = document.getElementById('dropdownContent');
    if (!dropdownContent) return;
    
    // Toggle show class
    dropdownContent.classList.toggle('show');
    
    console.log('Dropdown toggled, visible:', dropdownContent.classList.contains('show'));
}

// Close dropdown
function closeDropdown() {
    const dropdownContent = document.getElementById('dropdownContent');
    if (dropdownContent) {
        dropdownContent.classList.remove('show');
    }
}

// Close dropdowns when clicking outside
window.addEventListener('click', function(event) {
    if (!event.target.matches('.dropbtn') && !event.target.closest('.dropdown-content')) {
        closeDropdown();
    }
});

// Handle window resize
window.addEventListener('resize', function() {
    const navMenu = document.getElementById('navMenu');
    const hamburger = document.querySelector('.hamburger');
    
    if (window.innerWidth > 768 && navMenu && hamburger) {
        // Reset menu for desktop view
        navMenu.classList.remove('active');
        hamburger.classList.remove('active');
        document.body.style.overflow = '';
    }
});

// Initialize on DOM content loaded
document.addEventListener('DOMContentLoaded', function() {
    console.log('Page initialized');
    
    // Set initial aria states
    const hamburger = document.querySelector('.hamburger');
    if (hamburger) {
        hamburger.setAttribute('aria-expanded', 'false');
    }
    
    // Fix any <br> tags in dropdown
    const dropdownContent = document.getElementById('dropdownContent');
    if (dropdownContent) {
        const brTags = dropdownContent.querySelectorAll('br');
        brTags.forEach(br => br.remove());
    }
});

// Add this code to ensure menu items display properly
// This runs after a small delay to ensure the DOM is fully loaded
setTimeout(function() {
    const navMenu = document.getElementById('navMenu');
    if (navMenu && navMenu.classList.contains('active')) {
        // Force display all menu items if menu is already active
        const menuItems = navMenu.querySelectorAll('.menu li');
        menuItems.forEach((item, index) => {
            item.style.opacity = '1';
            item.style.visibility = 'visible';
            item.style.display = 'block';
        });
        
        console.log('Applied forced visibility to', menuItems.length, 'menu items');
    }
}, 300);

// Add this to the end of your user.js file

// Mobile menu functions - completely separate from regular menu
function toggleMobileMenu() {
  const mobileNav = document.getElementById('mobileNavPanel');
  const body = document.body;
  
  if (!mobileNav) return;
  
  // Toggle active class
  mobileNav.classList.toggle('active');
  
  // Toggle body scroll
  if (mobileNav.classList.contains('active')) {
    body.style.overflow = 'hidden';
    
    // Create overlay if it doesn't exist
    if (!document.querySelector('.mobile-menu-overlay')) {
      const overlay = document.createElement('div');
      overlay.className = 'mobile-menu-overlay';
      document.body.appendChild(overlay);
      
      // Add click event to close menu when clicking overlay
      overlay.addEventListener('click', toggleMobileMenu);
      
      // Delay adding active class for animation
      setTimeout(() => {
        overlay.classList.add('active');
      }, 10);
    } else {
      document.querySelector('.mobile-menu-overlay').classList.add('active');
    }
  } else {
    body.style.overflow = '';
    
    // Remove active class from overlay
    const overlay = document.querySelector('.mobile-menu-overlay');
    if (overlay) {
      overlay.classList.remove('active');
      
      // Remove overlay after transition
      setTimeout(() => {
        if (overlay && overlay.parentNode) {
          overlay.parentNode.removeChild(overlay);
        }
      }, 300);
    }
    
    // Close mobile dropdown if open
    closeMobileDropdown();
  }
}

// Toggle mobile dropdown
function toggleMobileDropdown() {
  const dropdown = document.getElementById('mobileDropdown');
  if (!dropdown) return;
  
  dropdown.classList.toggle('show');
  
  // Animation fixes
  if (dropdown.classList.contains('show')) {
    // Make sure all links are visible
    const links = dropdown.querySelectorAll('a');
    links.forEach(link => {
      link.style.display = 'block';
    });
  }
}

// Close mobile dropdown
function closeMobileDropdown() {
  const dropdown = document.getElementById('mobileDropdown');
  if (dropdown) {
    dropdown.classList.remove('show');
  }
}

// Close dropdown when clicking outside
document.addEventListener('click', function(event) {
  const profileBtn = document.querySelector('.mobile-profile-btn');
  const dropdown = document.getElementById('mobileDropdown');
  
  if (profileBtn && dropdown && !profileBtn.contains(event.target) && !dropdown.contains(event.target)) {
    dropdown.classList.remove('show');
  }
});

// Initialize mobile menu when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
  // Add swipe gesture support for mobile menu (optional)
  const mobileNav = document.getElementById('mobileNavPanel');
  
  if (mobileNav) {
    let touchStartX = 0;
    let touchEndX = 0;
    
    mobileNav.addEventListener('touchstart', e => {
      touchStartX = e.changedTouches[0].screenX;
    }, false);
    
    mobileNav.addEventListener('touchend', e => {
      touchEndX = e.changedTouches[0].screenX;
      handleSwipe();
    }, false);
    
    function handleSwipe() {
      if (touchStartX - touchEndX > 50) {
        // Swipe left - close menu
        toggleMobileMenu();
      }
    }
  }
});