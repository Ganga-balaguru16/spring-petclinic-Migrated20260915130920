```java
package org.springframework.samples.petclinic.owner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ExtendWith(MockitoExtension.class)
class OwnerControllerTest {

    @Mock
    private OwnerRepository owners;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private Model model;

    @Mock
    private WebDataBinder webDataBinder;

    @InjectMocks
    private OwnerController ownerController;

    private Owner validOwner;

    @BeforeEach
    void setUp() {
        validOwner = new Owner();
        validOwner.setId(1);
        validOwner.setFirstName("John");
        validOwner.setLastName("Doe");
        validOwner.setAddress("123 Main St");
        validOwner.setCity("Springfield");
        validOwner.setTelephone("123-456-7890");
    }

    @Test
    @DisplayName("Given a WebDataBinder, when setAllowedFields is called, then disallowed fields are set")
    void givenWebDataBinder_whenSetAllowedFields_thenDisallowedFieldsAreSet() {
        // Arrange
        // Act
        ownerController.setAllowedFields(webDataBinder);

        // Assert
        verify(webDataBinder).setDisallowedFields("id", "*.id");
    }

    @Test
    @DisplayName("Given no parameters, when initCreationForm is called, then returns createOrUpdateOwnerForm view")
    void givenNoParameters_whenInitCreationForm_thenReturnCreateOrUpdateOwnerFormView() {
        // Arrange
        // Act
        String viewName = ownerController.initCreationForm();

        // Assert
        assertEquals("owners/createOrUpdateOwnerForm", viewName);
    }

    @Test
    @DisplayName("Given valid owner and no errors, when processCreationForm is called, then saves owner and redirects")
    void givenValidOwnerAndNoErrors_whenProcessCreationForm_thenSavesOwnerAndRedirects() {
        // Arrange
        when(bindingResult.hasErrors()).thenReturn(false);
        when(owners.save(any(Owner.class))).thenReturn(validOwner);

        // Act
        String viewName = ownerController.processCreationForm(validOwner, bindingResult, redirectAttributes);

        // Assert
        assertEquals("redirect:/owners/1", viewName);
        verify(owners, times(1)).save(validOwner);
        verify(redirectAttributes, times(1)).addFlashAttribute("message", "New Owner Created");
    }

    @Test
    @DisplayName("Given valid owner with errors, when processCreationForm is called, then returns form view with error")
    void givenValidOwnerWithErrors_whenProcessCreationForm_thenReturnFormViewWithError() {
        // Arrange
        when(bindingResult.hasErrors()).thenReturn(true);

        // Act
        String viewName = ownerController.processCreationForm(validOwner, bindingResult, redirectAttributes);

        // Assert
        assertEquals("owners/createOrUpdateOwnerForm", viewName);
        verify(owners, never()).save(any(Owner.class));
        verify(redirectAttributes, times(1)).addFlashAttribute("error", "There was an error in creating the owner.");
    }

    @Test
    @DisplayName("Given null owner, when processCreationForm is called, then returns form view with error")
    void givenNullOwner_whenProcessCreationForm_thenReturnFormViewWithError() {
        // Arrange
        when(bindingResult.hasErrors()).thenReturn(true);

        // Act
        String viewName = ownerController.processCreationForm(null, bindingResult, redirectAttributes);

        // Assert
        assertEquals("owners/createOrUpdateOwnerForm", viewName);
        verify(owners, never()).save(any(Owner.class));
        verify(redirectAttributes, times(1)).addFlashAttribute("error", "There was an error in creating the owner.");
    }

    @Test
    @DisplayName("Given no parameters, when initFindForm is called, then returns findOwners view")
    void givenNoParameters_whenInitFindForm_thenReturnFindOwnersView() {
        // Arrange
        // Act
        String viewName = ownerController.initFindForm();

        // Assert
        assertEquals("owners/findOwners", viewName);
    }

    @Test
    @DisplayName("Given page 1 and empty last name, when processFindForm is called with no results, then returns findOwners view with rejection")
    void givenPage1AndEmptyLastName_whenProcessFindFormWithNoResults_thenReturnFindOwnersViewWithRejection() {
        // Arrange
        Owner owner = new Owner();
        owner.setLastName(null);
        Page<Owner> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);
        when(owners.findByLastNameStartingWith(eq(""), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        String viewName = ownerController.processFindForm(1, owner, bindingResult, model);

        // Assert
        assertEquals("owners/findOwners", viewName);
        verify(bindingResult, times(1)).rejectValue("lastName", "notFound", "not found");
    }

    @Test
    @DisplayName("Given page 1 and last name 'Doe', when processFindForm is called with one result, then redirects to owner")
    void givenPage1AndLastNameDoe_whenProcessFindFormWithOneResult_thenRedirectsToOwner() {
        // Arrange
        Owner owner = new Owner();
        owner.setLastName("Doe");
        Page<Owner> singlePage = new PageImpl<>(List.of(validOwner), PageRequest.of(0, 5), 1);
        when(owners.findByLastNameStartingWith(eq("Doe"), any(Pageable.class))).thenReturn(singlePage);

        // Act
        String viewName = ownerController.processFindForm(1, owner, bindingResult, model);

        // Assert
        assertEquals("redirect:/owners/1", viewName);
        verify(bindingResult, never()).rejectValue(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Given page 1 and last name 'Doe', when processFindForm is called with multiple results, then returns ownersList view")
    void givenPage1AndLastNameDoe_whenProcessFindFormWithMultipleResults_thenReturnOwnersListView() {
        // Arrange
        Owner owner = new Owner();
        owner.setLastName("Doe");
        Owner owner2 = new Owner();
        owner2.setId(2);
        owner2.setFirstName("Jane");
        owner2.setLastName("Doe");
        Page<Owner> multiPage = new PageImpl<>(List.of(validOwner, owner2), PageRequest.of(0, 5), 2);
        when(owners.findByLastNameStartingWith(eq("Doe"), any(Pageable.class))).thenReturn(multiPage);

        // Act
        String viewName = ownerController.processFindForm(1, owner, bindingResult, model);

        // Assert
        assertEquals("owners/ownersList", viewName);
        verify(model, times(1)).addAttribute("currentPage", 1);
        verify(model, times(1)).addAttribute("totalPages", 1);
        verify(model, times(1)).addAttribute("totalItems", 2);
        verify(model, times(1)).addAttribute("listOwners", List.of(validOwner, owner2));
    }

    @Test
    @DisplayName("Given page 2 and last name 'Doe', when processFindForm is called with multiple results, then returns ownersList view with correct page")
    void givenPage2AndLastNameDoe_whenProcessFindFormWithMultipleResults_thenReturnOwnersListViewWithCorrectPage() {
        // Arrange
        Owner owner = new Owner();
        owner.setLastName("Doe");
        Owner owner2 = new Owner();
        owner2.setId(2);
        owner2.setFirstName("Jane");
        owner2.setLastName("Doe");
        Page<Owner> multiPage = new PageImpl<>(List.of(validOwner, owner2), PageRequest.of(1, 5), 2);
        when(owners.findByLastNameStartingWith(eq("Doe"), any(Pageable.class))).thenReturn(multiPage);

        // Act
        String viewName = ownerController.processFindForm