package com.northstar.crm.viewCustomer;

import com.northstar.crm.domain.Customer;
import com.northstar.crm.domain.CustomerId;
import com.northstar.crm.domain.CustomerNotFoundException;
import com.northstar.crm.domain.CustomerStatus;
import com.northstar.crm.viewCustomer.dto.CustomerView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewCustomerServiceTest {

    @Mock
    ViewCustomerRepository repository;

    @Test
    void returnsTheCustomerAsAView() {
        Customer amina = Customer.registerProspect("Amina Khan", "amina.khan@example.com");
        amina.activate();
        UUID id = amina.getCustomerId().value();
        when(repository.findById(new CustomerId(id))).thenReturn(Optional.of(amina));

        CustomerView view = new ViewCustomerService(repository).get(id);

        assertThat(view).isEqualTo(
            new CustomerView(id, "Amina Khan", "amina.khan@example.com", CustomerStatus.ACTIVE));
    }

    @Test
    void unknownCustomerThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(new CustomerId(id))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ViewCustomerService(repository).get(id))
            .isInstanceOf(CustomerNotFoundException.class)
            .hasMessageContaining(id.toString());
    }
}
