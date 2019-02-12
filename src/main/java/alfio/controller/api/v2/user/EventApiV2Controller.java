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

import alfio.controller.api.support.EventListItem;
import alfio.manager.EventManager;
import alfio.model.Event;
import alfio.repository.EventDescriptionRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/v2/public/")
@AllArgsConstructor
public class EventApiV2Controller {

    private final EventManager eventManager;
    private final EventDescriptionRepository eventDescriptionRepository;

    //TODO: copy EventListItem to v2.model
    @RequestMapping("events")
    public ResponseEntity<List<EventListItem>> listEvents(HttpServletRequest request) {
        var events = eventManager.getPublishedEvents();
        var eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        var descriptionsByEventId = eventDescriptionRepository.findByEventIdsAsMap(eventIds);

        var res = events.stream()
            .map(ev -> new EventListItem(ev, request.getContextPath(), descriptionsByEventId.get(ev.getId())))
            .collect(Collectors.toList());

        return new ResponseEntity<>(res, getCorsHeaders(), HttpStatus.OK);
    }


    private static HttpHeaders getCorsHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        return headers;
    }
}
