type Publishable = { publicationStatus: string; isInternship: boolean; paymentStatus: string; expiresAt: string | null; applicationDeadline: string | null };
export function isActiveListing(item: Publishable, now = Date.now()) {
 const available = (date: string | null) => !date || (Number.isFinite(Date.parse(date)) && Date.parse(date.length===10?date+'T23:59:59':date)>=now);
 return item.publicationStatus==='PUBLISHED' && (!item.isInternship || item.paymentStatus==='PAID') && available(item.expiresAt) && available(item.applicationDeadline);
}
