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

import alfio.controller.ReservationController;
import alfio.controller.form.ContactAndTicketsForm;
import alfio.controller.form.PaymentForm;
import alfio.controller.support.SessionUtil;
import alfio.controller.support.TicketDecorator;
import alfio.manager.TicketReservationManager;
import alfio.model.TicketCategory;
import alfio.repository.TicketReservationRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v2/public/")
public class ReservationApiV2Controller {

    private final ReservationController reservationController;
    private final TicketReservationManager ticketReservationManager;
    private final TicketReservationRepository ticketReservationRepository;

    @AllArgsConstructor
    @Getter
    public static class TicketsByTicketCategory {
        private final TicketCategory ticketCategory;
        private final List<TicketDecorator> tickets;
    }

    @GetMapping("/event/{eventName}/reservation/{reservationId}/book")
    public ResponseEntity<Map<String, ?>> getBookingInfo(@PathVariable("eventName") String eventName,
                                                         @PathVariable("reservationId") String reservationId,
                                                         Model model,
                                                         Locale locale) {
        var res = reservationController.showBookingPage(eventName, reservationId, model, locale);
        //FIXME temporary
        model.addAttribute("viewState", res);

        //overwrite with a better model object...
        var ticketsByCategory = (List<Pair<TicketCategory, List<TicketDecorator>>>) model.asMap().get("ticketsByCategory");
        if (ticketsByCategory != null) {
            model.addAttribute("ticketsByCategory", ticketsByCategory.stream().map(a -> new TicketsByTicketCategory(a.getKey(), a.getValue())).collect(Collectors.toList()));
        }
        return new ResponseEntity<>(model.asMap(), HttpStatus.OK);
    }

    @DeleteMapping("/event/{eventName}/reservation/{reservationId}")
    public ResponseEntity<Boolean> cancelPendingReservation(@PathVariable("eventName") String eventName,
                                                            @PathVariable("reservationId") String reservationId,
                                                            HttpServletRequest request) {

        //FIXME check precondition (see ReservationController.redirectIfNotValid)
        ticketReservationManager.cancelPendingReservation(reservationId, false, null);
        SessionUtil.cleanupSession(request);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @PostMapping("/event/{eventName}/reservation/{reservationId}/back-to-booking")
    public ResponseEntity<Boolean> backToBook(@PathVariable("eventName") String eventName,
                                              @PathVariable("reservationId") String reservationId) {

        //FIXME check precondition (see ReservationController.redirectIfNotValid)

        ticketReservationRepository.updateValidationStatus(reservationId, false);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @PostMapping("/event/{eventName}/reservation/{reservationId}")
    public ResponseEntity<Map<String, ?>> handleReservation(@PathVariable("eventName") String eventName,
                                  @PathVariable("reservationId") String reservationId,
                                  @RequestBody  PaymentForm paymentForm,
                                  BindingResult bindingResult,
                                  Model model,
                                  HttpServletRequest request,
                                  Locale locale,
                                  RedirectAttributes redirectAttributes,
                                  HttpSession session) {
        //FIXME check precondition (see ReservationController.redirectIfNotValid)

        var res = reservationController.handleReservation(eventName, reservationId, paymentForm,
            bindingResult, model, request, locale, redirectAttributes,
            session);
        Map<String, Object> mapRes = new HashMap<>();
        mapRes.put("viewState", res);
        return new ResponseEntity<>(mapRes, HttpStatus.OK);
    }

    @PostMapping("/event/{eventName}/reservation/{reservationId}/validate-to-overview")
    public ResponseEntity<Map<String, ?>> validateToOverview(@PathVariable("eventName") String eventName,
                                   @PathVariable("reservationId") String reservationId,
                                   @RequestBody ContactAndTicketsForm contactAndTicketsForm,
                                   BindingResult bindingResult,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {

        //FIXME check precondition (see ReservationController.redirectIfNotValid)

        var res = reservationController.validateToOverview(eventName, reservationId, contactAndTicketsForm, bindingResult, request, redirectAttributes);
        var model = new HashMap<String, Object>();
        model.put("viewState", res);
        //model.put("bindingResult", bindingResult.getModel()); <- cause 400 error
        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @GetMapping("/event/{eventName}/reservation/{reservationId}/overview")
    public ResponseEntity<Map<String, ?>> showOverview(@PathVariable("eventName") String eventName,
                               @PathVariable("reservationId") String reservationId,
                               Locale locale,
                               Model model,
                               HttpSession session) {
        var res = reservationController.showOverview(eventName, reservationId, locale, model, session);
        model.addAttribute("viewState", res);
        return new ResponseEntity<>(model.asMap(), HttpStatus.OK);
    }

}
