/**
 * This file is part of alf.io.
 *
 * alf.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * alf.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with alf.io.  If not, see <http://www.gnu.org/licenses/>.
 */
package alfio.controller.api.v2.user;

import alfio.controller.EventController;
import alfio.controller.form.ReservationForm;
import alfio.manager.EventManager;
import alfio.manager.system.ConfigurationManager;
import alfio.model.ContentLanguage;
import alfio.model.Event;
import alfio.model.modification.support.LocationDescriptor;
import alfio.model.result.ValidationResult;
import alfio.model.system.Configuration;
import alfio.model.system.ConfigurationKeys;
import alfio.model.user.Organization;
import alfio.repository.EventRepository;
import alfio.repository.user.OrganizationRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;


@RestController
@RequestMapping("/api/v2/public/")
@AllArgsConstructor
public class EventApiV2Controller {

    private final EventController eventController;
    private final EventManager eventManager;
    private final EventRepository eventRepository;
    private final ConfigurationManager configurationManager;
    private final OrganizationRepository organizationRepository;


    @GetMapping("tmp/events")
    public ResponseEntity<Map<String, ?>> listEvents(Model model, Locale locale, HttpServletRequest request) {
        if(!"/event/event-list".equals(eventController.listEvents(model, locale))) {
            model.addAttribute("singleEvent", true);
            model.addAttribute("eventShortName", eventManager.getPublishedEvents().get(0).getShortName());
        }
        return new ResponseEntity<>(model.asMap(), getCorsHeaders(), HttpStatus.OK);
    }

    @GetMapping("event/{eventName}")
    public ResponseEntity<EventWithAdditionalInfo> getEvent(@PathVariable("eventName") String eventName) {
        return eventRepository.findOptionalByShortName(eventName).filter(e -> e.getStatus() != Event.Status.DISABLED)//
            .map(event -> {

                Organization organization = organizationRepository.getById(event.getOrganizationId());

                Map<ConfigurationKeys, Optional<String>> geoInfoConfiguration = configurationManager.getStringConfigValueFrom(
                    Configuration.from(event, ConfigurationKeys.MAPS_PROVIDER),
                    Configuration.from(event, ConfigurationKeys.MAPS_CLIENT_API_KEY),
                    Configuration.from(event, ConfigurationKeys.MAPS_HERE_APP_ID),
                    Configuration.from(event, ConfigurationKeys.MAPS_HERE_APP_CODE));
                LocationDescriptor ld = LocationDescriptor.fromGeoData(event.getLatLong(), TimeZone.getTimeZone(event.getTimeZone()), geoInfoConfiguration);
                return new ResponseEntity<>(new EventWithAdditionalInfo(event, ld, organization), getCorsHeaders(), HttpStatus.OK);
            })
            .orElseGet(() -> ResponseEntity.notFound().headers(getCorsHeaders()).build());
    }

    @AllArgsConstructor
    public static class EventWithAdditionalInfo {
        private final Event event;
        private final LocationDescriptor locationDescriptor;
        private final Organization organization;


        public String getDisplayName() {
            return event.getDisplayName();
        }

        public boolean getFileBlobIdIsPresent() {
            return event.getFileBlobIdIsPresent();
        }

        public String getFileBlobId() {
            return event.getFileBlobId();
        }

        public String getImageUrl() {
            return event.getImageUrl();
        }

        public String getWebsiteUrl() {
            return event.getWebsiteUrl();
        }

        public List<ContentLanguage> getContentLanguages() {
            return event.getContentLanguages();
        }

        public LocationDescriptor getLocationDescriptor() {
            return locationDescriptor;
        }

        public String getOrganizationName() {
            return organization.getName();
        }

        public String getOrganizationEmail() {
            return organization.getEmail();
        }
    }

    @GetMapping("tmp/event/{eventName}")
    public ResponseEntity<Map<String, ?>> getTmpEvent(@PathVariable("eventName") String eventName,
                                          Model model, HttpServletRequest request, Locale locale) {
        if ("/event/show-event".equals(eventController.showEvent(eventName, model, request, locale))) {
            return new ResponseEntity<>(model.asMap(), getCorsHeaders(), HttpStatus.OK);
        } else {
            return ResponseEntity.notFound().headers(getCorsHeaders()).build();
        }
    }

    @PostMapping("tmp/event/{eventName}/promoCode/{promoCode}")
    @ResponseBody
    public ValidationResult savePromoCode(@PathVariable("eventName") String eventName,
                                          @PathVariable("promoCode") String promoCode,
                                          Model model,
                                          HttpServletRequest request) {
        return eventController.savePromoCode(eventName, promoCode, model, request);
    }

    @PostMapping(value = "tmp/event/{eventName}/reserve-tickets")
    public ResponseEntity<Map<String, ?>> reserveTicket(@PathVariable("eventName") String eventName,
                                               @RequestBody ReservationForm reservation,
                                               BindingResult bindingResult,
                                               ServletWebRequest request,
                                               RedirectAttributes redirectAttributes,
                                               Locale locale) {

        String redirectResult = eventController.reserveTicket(eventName, reservation, bindingResult, request, redirectAttributes, locale);

        if (bindingResult.hasErrors()) {
            return new ResponseEntity<>(redirectAttributes.asMap(), getCorsHeaders(), HttpStatus.OK);
        } else {
            String reservationIdentifier = redirectResult
                .substring(redirectResult.lastIndexOf("reservation/")+"reservation/".length())
                .replace("/book", "");
            redirectAttributes.addAttribute("reservationIdentifier", reservationIdentifier);
            return new ResponseEntity<>(redirectAttributes.asMap(), HttpStatus.OK);
        }

    }


    private static HttpHeaders getCorsHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        return headers;
    }
}
