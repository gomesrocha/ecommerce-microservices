package br.com.ecommerce.resource;

import br.com.ecommerce.dto.NotificationRequest;
import br.com.ecommerce.dto.NotificationResponse;
import br.com.ecommerce.service.NotificationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationResource {

    @Inject
    NotificationService service;

    @POST
    public NotificationResponse create(@Valid NotificationRequest request) {
        return service.create(request);
    }

    @GET
    @Path("/{id}")
    public NotificationResponse findById(@PathParam("id") Long id) {
        return service.findById(id);
    }

    @GET
    @Path("/user/{userId}")
    public List<NotificationResponse> listByUser(@PathParam("userId") Long userId) {
        return service.listByUser(userId);
    }

    @GET
    @Path("/user/{userId}/unread")
    public List<NotificationResponse> listUnreadByUser(@PathParam("userId") Long userId) {
        return service.listUnreadByUser(userId);
    }

    @PUT
    @Path("/{id}/read")
    public NotificationResponse markAsRead(@PathParam("id") Long id) {
        return service.markAsRead(id);
    }
}