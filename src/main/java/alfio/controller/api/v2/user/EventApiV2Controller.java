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
import java.util.Locale;
import java.util.Map;


@RestController
@RequestMapping("/api/v2/public/")
@AllArgsConstructor
public class EventApiV2Controller {

    private final EventController eventController;


    @GetMapping("events")
    public ResponseEntity<Map<String, ?>> listEvents(Model model, Locale locale, HttpServletRequest request) {
        eventController.listEvents(model, locale);
        return new ResponseEntity<>(model.asMap(), getCorsHeaders(), HttpStatus.OK);
    }

    @GetMapping("/event/{eventName}")
    public ResponseEntity<Map<String, ?>> getEvent(@PathVariable("eventName") String eventName,
                                          Model model, HttpServletRequest request, Locale locale) {
        if ("/event/show-event".equals(eventController.showEvent(eventName, model, request, locale))) {
            return new ResponseEntity<>(model.asMap(), getCorsHeaders(), HttpStatus.OK);
        } else {
            return ResponseEntity.notFound().headers(getCorsHeaders()).build();
        }
    }

    @PostMapping(value = "/event/{eventName}/reserve-tickets")
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
            String reservationIdentifier = redirectResult.substring(redirectResult.length()).replace("/book", "");
            redirectAttributes.addAttribute("reservationIdentifier", reservationIdentifier);
            return new ResponseEntity<>(redirectAttributes.asMap(), getCorsHeaders(), HttpStatus.OK);
        }

    }


    private static HttpHeaders getCorsHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        return headers;
    }
}
