# ADR 0001: Open-source Premium product model

- Status: Accepted for implementation
- Date: 2026-09-16
- Owners: TuneFlow maintainers
- Parent roadmap: [#172](https://github.com/Venkatpandey/TuneFlow/issues/172)
- Decision issue: [#173](https://github.com/Venkatpandey/TuneFlow/issues/173)

## Context

TuneFlow is MIT-licensed and already distributes free Android TV and Fire TV builds. The project wants to fund continued development without removing existing functionality, weakening playback reliability, or pretending that source-visible client checks are strong DRM.

The MIT license permits people to use, modify, distribute, sublicense, and sell copies of the software. A fork can therefore remove a local feature check. The commercial model must accept that property instead of hiding secrets or essential free behavior in the client.

## Decision

TuneFlow will use a fully open-source, official-build model:

- Existing source and released versions remain MIT-licensed.
- New app and service code remains source-visible unless a future ADR explicitly changes the policy prospectively.
- Free playback, library access, search, playlists, favorites, accessibility, security fixes, reliability fixes, and updates are never paywalled.
- Existing self-hosted preferred-video mapping remains free and supported.
- Premium Lifetime is a one-time purchase for new device-local convenience and customization.
- Premium Cloud is a subscription only for features with recurring TuneFlow-operated hosting cost.
- Official builds verify purchases and receive signed entitlements. Self-built forks may change local gates, but cannot forge access to TuneFlow-operated services.
- No credentials, purchase-verification secrets, or signing keys are embedded in the APK.
- Billing or entitlement failure always falls back to the free app without interrupting playback.

This is not an open-core model. Payment buys supported official distribution, easy restoration, and hosted value; it does not revoke the freedoms granted by the MIT license.

## Product tiers

### Free

Free includes every capability already released before the Premium launch, including:

- Navidrome login, browsing, search, favorites, playlists, queueing, playback, resume, lyrics, and video integration.
- The current playback screensaver and all core focus, motion, accessibility, and navigation behavior.
- Local settings and the user-operated preferred-video service.
- Security, compatibility, stability, migration, and update work.

### Premium Lifetime

Premium Lifetime may include these new capabilities, delivered independently:

| Capability ID | Product feature | Roadmap |
|---|---|---|
| `account.multi_server` | Multiple saved servers and account switching | #182 |
| `home.customization` | Configurable Home rows and startup destination | #185 |
| `mix.smart_rules` | Configurable device-local smart mixes | #184 |
| `household.profiles` | Household profiles and local PIN protection | #183 |
| `audio.replay_gain` | ReplayGain loudness normalization | #188 |
| `theme.packs` | Selectable visual theme packs | #186 |
| `ambient.cinematic` | Enhanced ambient Now Playing presentation | #187 |
| `audio.crossfade` | Configurable audio crossfade | #189 |
| `audio.equalizer` | Equalizer presets | #190 |

The existing basic screensaver remains free. Only the new cinematic ambient presentation and its customization use `ambient.cinematic`.

### Premium Cloud

| Capability ID | Hosted feature | Roadmap |
|---|---|---|
| `cloud.settings_sync` | Encrypted settings backup and sync | #191 |
| `cloud.video_mapping` | Managed preferred-video mapping and history | #192 |
| `cloud.phone_remote` | Paired-phone remote control | #193 |
| `cloud.party_queue` | Shared party queue | #194 |

Cloud features must describe retention and deletion behavior before launch. Users may continue using the existing self-hosted alternatives where available.

## Entitlement contract

Entitlements use capability IDs, not UI names or store product IDs. Client code asks for capabilities and does not branch directly on billing providers.

The initial entitlement claims are:

- `tier.premium_lifetime`
- `tier.premium_cloud`
- The granular capability IDs listed above

An entitlement records issuer, subject, capabilities, issued time, optional expiry, schema version, and signature. Hosted APIs independently authorize Cloud capabilities; they never trust an APK-only boolean.

Debug and local beta builds may grant all device-local capabilities for testing. Stable official builds must use verified entitlements. This override must be compile-time, visibly identified as non-production, and unavailable in a stable release artifact.

## Products and ownership

Logical product keys are stable across providers:

| Product key | Type | Grants |
|---|---|---|
| `premium_lifetime_v1` | One-time | `tier.premium_lifetime` and all current Lifetime capabilities |
| `premium_cloud_monthly_v1` | Recurring | `tier.premium_cloud` while active |
| `premium_cloud_yearly_v1` | Recurring | `tier.premium_cloud` while active |

Prices and currency are owned by the sales channel and must not be hard-coded in the app or entitlement service. Google Play, Amazon Appstore, and web checkout map their own SKU identifiers to these logical product keys in deployment configuration.

Lifetime ownership is permanent for the purchasing account. New Lifetime capabilities can be added to `premium_lifetime_v1`; removing already-granted capabilities requires a new ADR and migration plan. A Cloud subscription does not replace Lifetime ownership.

## Restore and portability

- Store purchases restore through the same store account.
- Web purchases restore through TuneFlow device activation.
- Cross-channel restoration requires linking purchases to the same TuneFlow account; it is not assumed automatically.
- A signed Lifetime entitlement remains usable offline after successful verification. A temporary verifier or billing outage does not remove it.
- Refunds, chargebacks, and store revocations take effect after the next successful verification. Offline playback and free features remain available.
- Cloud access uses the provider's reported subscription period plus a seven-day entitlement grace period. During a verifier outage, cached Cloud entitlement remains valid through that grace period.
- Device loss does not consume ownership. Device-count abuse controls, if later needed, require a separate ADR and a user-visible recovery path.

## Refund, cancellation, and expiry

- Each channel's displayed terms and mandatory consumer protections control refunds.
- The web channel must publish refund terms, merchant identity, taxes, privacy terms, and support contact before accepting payment.
- Cancelling Cloud stops renewal but keeps access until the paid period ends.
- Subscription expiry disables only Cloud capabilities. Lifetime and Free capabilities remain unchanged.
- Refund or expiry never deletes local library, queue, settings, or Navidrome data.

Final customer-facing terms require legal and tax review in every launch market. This ADR is an engineering and product decision, not legal advice.

## Privacy and deletion

- Lifetime purchases store only the minimum receipt, account, entitlement, and fraud-prevention data required for verification and support.
- Device activation uses a random installation identifier, not Navidrome credentials or media-library data.
- Cloud data is opt-in, purpose-limited, encrypted in transit, and separated from Navidrome credentials wherever possible.
- Account deletion removes TuneFlow-hosted Cloud data and device links within the published retention window. Legally required transaction records may be retained separately and documented.
- Deleting a Cloud account does not delete data from Navidrome or a user-operated preferred-video service.
- Telemetry is not required to unlock either paid tier.

## Distribution channels

### GitHub APK

The GitHub-distributed official APK uses web checkout and TV device activation. It may link to the web purchase flow because it is not distributed by an app store. GitHub continues to publish the source and a usable free APK.

### Google Play

The Play build uses Google Play Billing for in-app sales of digital app functionality unless the build is enrolled in an applicable alternative-billing or external-links program. Region-specific exceptions must be configuration and policy decisions, not assumptions in shared app code.

### Amazon Appstore

The Amazon build uses Amazon In-App Purchasing for digital capabilities available inside the app unless Amazon explicitly approves another method.

Store builds must not display an external purchase call to action where store policy disallows it. A consumption-only variant may restore an existing entitlement without selling inside the app if the applicable store policy permits it.

## Official builds and project identity

- An official build is signed by a TuneFlow release certificate and published through a TuneFlow-controlled GitHub release, Play listing, or Amazon listing.
- Entitlement verification keys may be public; entitlement signing keys remain server-side.
- Modified builds must not present themselves as official or claim TuneFlow support.
- Referential use such as “based on TuneFlow” is welcome when it is clear that the build is unofficial.
- The TuneFlow name, logo, signing identity, store listings, and service domains identify the official project. A separate trademark policy and legal review are required before relying on enforcement beyond preventing user confusion.
- Nothing here narrows rights already granted with previously published MIT-licensed software or assets.

## Failure behavior

| Failure | Required behavior |
|---|---|
| Billing unavailable | Keep Free working; show a retryable purchase status |
| Entitlement verifier unavailable | Use valid cached entitlement and grace rules |
| Invalid or expired entitlement | Hide paid entry points cleanly; never stop playback |
| Cloud service unavailable | Keep local playback and Lifetime features working |
| Receipt pending | Show Pending; do not grant until provider confirmation |
| Refund or revocation | Remove only affected capabilities after verification |

## Consequences

### Positive

- The free app remains trustworthy and useful.
- Capability checks stay independent of stores and checkout providers.
- Open-source users can audit the complete client behavior.
- Hosted authorization remains enforceable even though local gates are bypassable.

### Trade-offs

- A technical user can self-build with local Premium features enabled.
- Cross-store restoration needs a TuneFlow account and backend.
- Official distribution, support, and Cloud reliability become the primary paid value.
- Store policy, tax, refund, and privacy work remain launch blockers.

## Required follow-up

1. Implement the provider-neutral capability model in #174.
2. Add distribution variants in #175 without adding purchase SDKs to GitHub builds.
3. Obtain legal and tax review before any public sale.
4. Publish customer-facing privacy, terms, refund, deletion, and support pages before checkout goes live.
5. Re-check store payment and external-link policies immediately before submission.

## References

- [Repository MIT license](../../LICENSE)
- [Open Source Initiative: MIT License](https://opensource.org/license/mit)
- [Google Play Payments policy](https://support.google.com/googleplay/android-developer/answer/9858738)
- [Amazon Appstore Monetization and Advertising Policy](https://developer.amazon.com/docs/policy-center/monetization.html)

