# TV navigation and Back behavior

TuneFlow treats remote Back as an escape through one visible layer at a time.

## Escape order

1. Close transient UI owned by the current screen, such as search editing, the Now Playing queue, or playlist detail.
2. Pop one nested destination, such as Album, Artist, Home Category, or Now Playing.
3. Return a root Albums, Playlists, or Search section to Home.
4. On Home, show the exit prompt. A second Back press inside the confirmation window exits the app.

Confirmed app exit deliberately stops playback, clears the player, stops the playback service, and removes the task.

## Destination rules

- Selecting an item pushes its detail destination onto the current stack.
- Selecting a top-level navigation-rail section resets the stack to that section root.
- Now Playing is pushed over the current destination, so Back returns to the exact underlying screen.
- Back from nested Artist to Album navigation returns to Artist before returning to its source screen.
- Back from playlist detail closes the detail panel before leaving Playlists.

## Focus rules

- Closing transient UI restores focus to the control or item that opened it.
- Popping detail restores the originating album, artist, playlist, or Home category action.
- Focus restoration scrolls the containing list or grid before requesting focus.
- If no restoration target exists, each screen uses its normal initial-focus behavior.

## Test expectations

Navigation changes should cover the affected stack transition with unit tests. Transient screen changes should also cover their escape decision or cancellation behavior. Remote-device verification should confirm that focus remains visible after every Back press.
